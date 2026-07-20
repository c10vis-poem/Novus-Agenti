package com.horizons.audio

import android.util.Log
import com.horizons.core.voice.SherpaOnnxTtsClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

enum class VoiceLoopState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * Voice loop: mic → Silero VAD (endpoint) → Moonshine STT → LLM → Kokoro TTS.
 *
 * VAD runs on BOTH ends of the conversation, on the recorder's live chunk
 * stream (the mic never stop/restarts mid-phase):
 *  - LISTENING: speech-end detection stops the recording (no hard timer).
 *  - SPEAKING: the mic stays open; >= 150 ms of user speech kills TTS
 *    mid-sentence and transitions straight back to LISTENING (barge-in).
 *  - [continuousMode]: when true the loop restarts from LISTENING automatically
 *    after each SPEAKING → IDLE cycle.
 */
class VoiceLoopController(
    private val scope: CoroutineScope,
    private val recorder: AudioRecorder,
    private val tts: SherpaOnnxTtsClient,
    private val engineStreamAudio: (ShortArray) -> Flow<String>,
    private val vad: VadDetector,
    private val continuousMode: Boolean = false,
) {
    private val _state = MutableStateFlow(VoiceLoopState.IDLE)
    val state: StateFlow<VoiceLoopState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var bargeInJob: Job? = null

    fun startLoop() {
        if (_state.value != VoiceLoopState.IDLE) return
        loopJob = scope.launch { runLoop() }
    }

    /** One-shot VAD recording for Mode C: mic → VAD endpoint → returns PCM. */
    suspend fun recordOnce(): ShortArray? {
        if (_state.value != VoiceLoopState.IDLE) return null
        _state.value = VoiceLoopState.LISTENING
        return try {
            if (recorder.start().isFailure) {
                _state.value = VoiceLoopState.IDLE
                return null
            }
            val pcm = collectUntilSilence()
            _state.value = VoiceLoopState.IDLE
            pcm
        } catch (e: CancellationException) {
            if (recorder.isRecording()) recorder.stop()
            _state.value = VoiceLoopState.IDLE
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "recordOnce error", e)
            if (recorder.isRecording()) recorder.stop()
            _state.value = VoiceLoopState.IDLE
            null
        }
    }

    private suspend fun runLoop() {
        try {
            while (true) {
                // ── LISTENING ────────────────────────────────────────────────
                _state.value = VoiceLoopState.LISTENING
                if (recorder.start().isFailure) {
                    _state.value = VoiceLoopState.IDLE; return
                }

                val pcm = collectUntilSilence()
                if (pcm == null || pcm.isEmpty()) {
                    _state.value = VoiceLoopState.IDLE; return
                }

                // ── THINKING (audio sent to ort_engine) ──────────────────
                _state.value = VoiceLoopState.THINKING
                val reply = StringBuilder()
                engineStreamAudio(pcm).collect { reply.append(it) }
                if (reply.isBlank()) {
                    Log.w(TAG, "Audio stream returned no usable reply: $reply")
                    if (continuousMode) continue else { _state.value = VoiceLoopState.IDLE; return }
                }

                // ── SPEAKING (mic stays open for barge-in) ───────────────────
                _state.value = VoiceLoopState.SPEAKING
                val bargedIn = speakWithBargeIn(reply.toString())

                if (bargedIn) {
                    // User interrupted — go straight back to LISTENING
                    Log.i(TAG, "Barge-in detected — restarting LISTENING")
                    continue
                }

                // Natural end of TTS
                if (continuousMode) continue else { _state.value = VoiceLoopState.IDLE; return }
            }
        } catch (e: CancellationException) {
            _state.value = VoiceLoopState.IDLE; throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Voice loop error", e)
            _state.value = VoiceLoopState.IDLE
        }
    }

    /**
     * Records PCM until VAD detects end-of-speech, consuming the recorder's
     * LIVE chunk stream — the mic runs continuously, no stop/restart windows,
     * no dropped audio between polls.
     *
     * Speech-end detection: when VAD returns false (silence) for
     * [SILENCE_GATE_MS] ms continuously AFTER seeing at least [MIN_SPEECH_MS]
     * ms of speech, recording stops.
     *
     * Hard timeout [MAX_RECORD_MS] prevents indefinite recording.
     */
    private suspend fun collectUntilSilence(): ShortArray? {
        val sampleRate = AudioRecorder.SAMPLE_RATE
        val silenceGateSamples = (sampleRate * SILENCE_GATE_MS / 1000)
        val minSpeechSamples = (sampleRate * MIN_SPEECH_MS / 1000)
        val maxSamples = (sampleRate * MAX_RECORD_MS / 1000).toInt()

        var totalSamples = 0
        var speechSamples = 0
        var silentSamples = 0
        var seenSpeech = false
        finishRequested = false

        try {
            // Timeout is the hard safety ceiling; VAD's silence gate is the
            // real endpoint. Also guards the no-emission case (recorder
            // stopped externally → no chunks → collect would hang forever).
            kotlinx.coroutines.withTimeoutOrNull(MAX_RECORD_MS) {
                recorder.chunks
                    .takeWhile { !finishRequested && totalSamples < maxSamples }
                    .collect { chunk ->
                        totalSamples += chunk.size
                        if (vad.isSpeech(chunk, sampleRate)) {
                            speechSamples += chunk.size
                            silentSamples = 0
                            seenSpeech = true
                        } else {
                            silentSamples += chunk.size
                            if (seenSpeech && speechSamples >= minSpeechSamples
                                && silentSamples >= silenceGateSamples
                            ) {
                                Log.d(TAG, "Speech ended after $speechSamples speech samples")
                                throw EndOfSpeech
                            }
                        }
                    }
            }
        } catch (_: EndOfSpeech) {
            // normal exit: silence gate tripped
        }

        // The accumulated recording (superset of what VAD saw) is the utterance.
        return recorder.stop().getOrNull()
    }

    /**
     * Ask an in-flight LISTENING phase to wrap up now (e.g. the user tapped
     * the mic again). The recording is kept and transcribed as usual.
     */
    fun finishListening() { finishRequested = true }

    @Volatile private var finishRequested = false

    /** Control-flow marker to break out of the chunk collect. */
    private object EndOfSpeech : Exception() {
        private fun readResolve(): Any = EndOfSpeech
    }

    /**
     * Speaks [text] while keeping the microphone open to detect barge-in.
     *
     * Returns true if the user interrupted (>= [BARGE_IN_SPEECH_MS] ms of
     * speech detected), false if TTS completed naturally.
     */
    private suspend fun speakWithBargeIn(text: String): Boolean {
        if (text.isBlank()) return false

        var bargeInDetected = false

        // Start recording in background for barge-in detection
        if (recorder.start().isFailure) {
            // Can't open mic — just speak without barge-in capability
            tts.speak(text)
            return false
        }

        // Barge-in monitor: watch the LIVE chunk stream while TTS plays; the
        // mic runs continuously (no stop/restart). Enough cumulative speech →
        // kill TTS mid-sentence.
        bargeInJob = scope.launch {
            val sampleRate = AudioRecorder.SAMPLE_RATE
            val minBargeInSamples = (sampleRate * BARGE_IN_SPEECH_MS / 1000)
            var cumulativeSpeechSamples = 0

            recorder.chunks.collect { chunk ->
                if (vad.isSpeech(chunk, sampleRate)) {
                    cumulativeSpeechSamples += chunk.size
                    if (cumulativeSpeechSamples >= minBargeInSamples) {
                        Log.i(TAG, "Barge-in: $cumulativeSpeechSamples speech samples detected")
                        bargeInDetected = true
                        tts.stop()
                        throw kotlinx.coroutines.CancellationException("barge-in")
                    }
                }
            }
        }

        // Speak (suspends until utterance complete or tts.stop() called)
        tts.speak(text)

        // Cancel barge-in monitor if TTS ended naturally
        bargeInJob?.cancel()
        bargeInJob = null

        // Ensure recorder is stopped regardless of path
        if (recorder.isRecording()) {
            recorder.stop()
        }

        return bargeInDetected
    }

    fun stop() {
        bargeInJob?.cancel()
        bargeInJob = null
        loopJob?.cancel()
        loopJob = null
        if (recorder.isRecording()) {
            scope.launch { recorder.stop() }
        }
        tts.stop()
        vad.close()
        _state.value = VoiceLoopState.IDLE
    }

    companion object {
        const val TAG = "VoiceLoopController"
        const val MAX_RECORD_MS = 8_000L

        /** Silence duration after speech required to end listening (ms). */
        private const val SILENCE_GATE_MS = 500L

        /** Minimum speech duration required before silence gate activates (ms). */
        private const val MIN_SPEECH_MS = 200L

        /** Cumulative speech duration during SPEAKING to trigger barge-in (ms). */
        private const val BARGE_IN_SPEECH_MS = 150L
    }
}
