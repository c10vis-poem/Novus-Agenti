package com.horizons.core.stt

import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * On-device STT via Sherpa-ONNX → Moonshine (tiny/base, English, int8).
 * The system-level transcription layer — same Sherpa-ONNX runtime already used
 * for Kokoro TTS, so no new native dependency.
 *
 * [modelDir] must contain the Moonshine files (see [MoonshineModelManager]):
 *   preprocess.onnx, encode.int8.onnx, uncached_decode.int8.onnx,
 *   cached_decode.int8.onnx, tokens.txt
 *
 * Call [init] once the model directory is ready.
 */
class MoonshineSttEngine(private val modelDir: String) : SttEngine {

    @Volatile private var recognizer: OfflineRecognizer? = null

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("STT · Moonshine (loading)")
    override val status: StateFlow<String> = _status.asStateFlow()

    override fun init() {
        if (recognizer != null) return
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = "$modelDir/preprocess.onnx",
                        encoder = "$modelDir/encode.int8.onnx",
                        uncachedDecoder = "$modelDir/uncached_decode.int8.onnx",
                        cachedDecoder = "$modelDir/cached_decode.int8.onnx",
                    ),
                    tokens = "$modelDir/tokens.txt",
                    numThreads = 2,
                    debug = false,
                    provider = "cpu",
                ),
            )
            recognizer = OfflineRecognizer(config = config)
            _ready.value = true
            _status.value = "STT · Moonshine"
            Log.i(TAG, "Moonshine STT ready ($modelDir)")
        } catch (e: Throwable) {
            _ready.value = false
            _status.value = "STT · Moonshine (failed)"
            Log.e(TAG, "Moonshine init failed", e)
        }
    }

    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): String =
        withContext(Dispatchers.Default) {
            val rec = recognizer ?: return@withContext ""
            if (pcm.isEmpty()) return@withContext ""
            try {
                // Sherpa-ONNX wants float samples in [-1, 1].
                val samples = FloatArray(pcm.size) { i -> pcm[i] / 32768f }
                val stream = rec.createStream()
                stream.acceptWaveform(samples, sampleRate)
                rec.decode(stream)
                val text = rec.getResult(stream).text
                stream.release()
                text.trim()
            } catch (e: Throwable) {
                Log.e(TAG, "transcribe failed", e)
                ""
            }
        }

    override fun close() {
        _ready.value = false
        runCatching { recognizer?.release() }
        recognizer = null
    }

    companion object {
        const val TAG = "MoonshineStt"
        const val SAMPLE_RATE = 16_000
    }
}
