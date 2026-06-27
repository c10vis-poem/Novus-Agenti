package com.horizons.assist

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log
import com.horizons.HorizonsApplication
import com.horizons.audio.AudioRecorder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * System speech recognizer backed by Gemma 4 12B (audio-direct via LiteRT-LM).
 *
 * When Horizons is selected as the device's default assistant / recognizer,
 * ANY app that calls SpeechRecognizer routes its mic capture through here
 * instead of Google's on-device recognizer — i.e. this replaces the stock STT.
 *
 * Flow: record PCM → WAV → llmRuntime.streamAudio() → transcript → results().
 *  - onStopListening(): caller signals "done speaking" → transcribe now.
 *  - onCancel():        caller aborts → drop recording, no result.
 *  - 12 s hard cap so a stuck caller can't hold the mic forever.
 */
class HorizonsRecognitionService : RecognitionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val app get() = applicationContext as HorizonsApplication

    private var job: Job? = null
    private var recorder: AudioRecorder? = null
    private var stopSignal: CompletableDeferred<Unit>? = null

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener ?: return
        // Only one recognition at a time.
        if (job?.isActive == true) {
            runCatching { listener.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY) }
            return
        }
        val rec = AudioRecorder(this)
        recorder = rec
        val stop = CompletableDeferred<Unit>()
        stopSignal = stop

        job = scope.launch {
            try {
                runCatching { listener.readyForSpeech(Bundle()) }
                if (rec.start().isFailure) {
                    runCatching { listener.error(SpeechRecognizer.ERROR_AUDIO) }
                    return@launch
                }
                runCatching { listener.beginningOfSpeech() }

                // Wait for the caller to stop us, or the 12 s safety cap.
                withTimeoutOrNull(MAX_LISTEN_MS) { stop.await() }

                val pcm = rec.stop().getOrNull()
                runCatching { listener.endOfSpeech() }
                if (pcm == null || pcm.isEmpty()) {
                    runCatching { listener.error(SpeechRecognizer.ERROR_NO_MATCH) }
                    return@launch
                }

                val text = app.transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
                if (text.isBlank() || text.startsWith("[")) {
                    runCatching { listener.error(SpeechRecognizer.ERROR_NO_MATCH) }
                    return@launch
                }

                val results = Bundle().apply {
                    putStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION,
                        arrayListOf(text),
                    )
                    putFloatArray(SpeechRecognizer.CONFIDENCE_SCORES, floatArrayOf(1.0f))
                }
                runCatching { listener.results(results) }
            } catch (t: Throwable) {
                Log.e(TAG, "recognition failed", t)
                runCatching { listener.error(SpeechRecognizer.ERROR_CLIENT) }
            } finally {
                recorder = null
                stopSignal = null
            }
        }
    }

    /** Caller is done speaking — end the listen window and transcribe what we have. */
    override fun onStopListening(listener: Callback?) {
        stopSignal?.complete(Unit)
    }

    /** Caller aborted — kill the job and drop the recording. */
    override fun onCancel(listener: Callback?) {
        job?.cancel()
        job = null
        val rec = recorder
        recorder = null
        stopSignal = null
        scope.launch { runCatching { rec?.stop() } }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HorizonsRecognition"
        private const val MAX_LISTEN_MS = 12_000L
    }
}
