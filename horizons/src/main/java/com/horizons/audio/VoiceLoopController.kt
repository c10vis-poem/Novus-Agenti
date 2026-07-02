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
import kotlinx.coroutines.launch

enum class VoiceLoopState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * Voice loop: mic → Qwen3.5-9B (audio via ort_engine) → TTS.
 *
 * G15 additions:
 *  - [vad] receives PCM chunks during LISTENING to detect speech-end (instead
 *    of a hard 8-second timeout).
 *  - During SPEAKING the mic stays open; if [vad] detects >= 150 ms of speech
 *    the TTS is stopped and the loop transitions back to LISTENING (barge-in).
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
                if (reply.isBlank() || reply.startsWith("[LiteRtRuntime")) {
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
     * Records PCM in chunks using VAD to determine end-of-speech.
     *
     * Collects chunks from [recorder]'s internal buffer by stopping and
     * restarting the recorder on silence detection. Returns null on error.
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

        val accumulated = ArrayList<ShortArray>()
        var totalSamples = 0
        var speechSamples = 0
        var silentSamples = 0
        var seenSpeech = false

        // Time-sliced VAD: every CHUNK_POLL_MS we stop the recorder, run VAD on the
        // newly accumulated samples, then restart — continues until silence gate or timeout.
        val startMs = System.currentTimeMillis()
        var lastCheckedSamples = 0

        while (true) {
            val elapsedMs = System.currentTimeMillis() - startMs
            if (elapsedMs >= MAX_RECORD_MS) {
                Log.d(TAG, "Max recording time reached")
                break
            }

            kotlinx.coroutines.delay(CHUNK_POLL_MS)

            val stopResult = recorder.stop()
            val allPcm = stopResult.getOrNull() ?: break

            if (allPcm.size > lastCheckedSamples) {
                val newChunk = allPcm.copyOfRange(lastCheckedSamples, allPcm.size)
                accumulated.add(newChunk)
                totalSamples += newChunk.size
                lastCheckedSamples = allPcm.size

                val isSpeaking = vad.isSpeech(newChunk, sampleRate)
                if (isSpeaking) {
                    speechSamples += newChunk.size
                    silentSamples = 0
                    seenSpeech = true
                } else {
                    silentSamples += newChunk.size
                    if (seenSpeech && speechSamples >= minSpeechSamples
                        && silentSamples >= silenceGateSamples
                    ) {
                        Log.d(TAG, "Speech ended after ${speechSamples}s samples")
                        break
                    }
                }
            }

            if (totalSamples >= maxSamples) break

            // Restart recorder for next window
            if (recorder.start().isFailure) break
        }

        // Final stop — collect any remaining data
        val finalStop = recorder.stop()
        val finalPcm = finalStop.getOrNull()
        if (finalPcm != null && finalPcm.size > lastCheckedSamples) {
            accumulated.add(finalPcm.copyOfRange(lastCheckedSamples, finalPcm.size))
            totalSamples += finalPcm.size - lastCheckedSamples
        }

        if (accumulated.isEmpty()) return ShortArray(0)

        val out = ShortArray(totalSamples)
        var offset = 0
        for (chunk in accumulated) {
            if (offset + chunk.size > out.size) break
            System.arraycopy(chunk, 0, out, offset, chunk.size)
            offset += chunk.size
        }
        return out
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

        // Launch barge-in monitor coroutine
        bargeInJob = scope.launch {
            val sampleRate = AudioRecorder.SAMPLE_RATE
            val minBargeInSamples = (sampleRate * BARGE_IN_SPEECH_MS / 1000)
            var cumulativeSpeechSamples = 0
            var lastCheckedSamples = 0

            while (true) {
                kotlinx.coroutines.delay(CHUNK_POLL_MS)
                val allPcm = recorder.stop().getOrNull() ?: break

                if (allPcm.size > lastCheckedSamples) {
                    val newChunk = allPcm.copyOfRange(lastCheckedSamples, allPcm.size)
                    lastCheckedSamples = allPcm.size

                    if (vad.isSpeech(newChunk, sampleRate)) {
                        cumulativeSpeechSamples += newChunk.size
                    }

                    if (cumulativeSpeechSamples >= minBargeInSamples) {
                        Log.i(TAG, "Barge-in: ${cumulativeSpeechSamples} speech samples detected")
                        bargeInDetected = true
                        tts.stop()
                        break
                    }
                }

                // Restart for next window
                if (recorder.start().isFailure) break
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

        /** Poll interval for VAD chunk checks (ms). */
        private const val CHUNK_POLL_MS = 20L

        /** Silence duration after speech required to end listening (ms). */
        private const val SILENCE_GATE_MS = 500L

        /** Minimum speech duration required before silence gate activates (ms). */
        private const val MIN_SPEECH_MS = 200L

        /** Cumulative speech duration during SPEAKING to trigger barge-in (ms). */
        private const val BARGE_IN_SPEECH_MS = 150L
    }
}
