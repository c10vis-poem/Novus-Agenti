package com.horizons.core.stt

import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-process Moonshine STT via sherpa-onnx (the same AAR that runs Kokoro TTS).
 *
 * THE one STT path — there is no media daemon (session-19 decision: the voice
 * layer runs in-process as the system accessibility / assistant / TTS-engine
 * surface; the daemon split was dead contract code and is deleted).
 *
 * Model files are user-loaded ("the user is the loader"), never downloaded:
 * import a sherpa-onnx Moonshine archive (e.g. sherpa-onnx-moonshine-tiny-en-int8)
 * via ModelImportActivity → extracted to [modelDir]. Expected files:
 *   preprocess.onnx · encode.onnx · uncached_decode.onnx · cached_decode.onnx · tokens.txt
 *
 * Voice flow: mic → Silero VAD (endpoint/barge-in) → THIS (transcribe) → LLM → TTS.
 */
class MoonshineSttEngine(context: Context) : SttEngine {

    val modelDir: File = File(context.filesDir, "sherpa_stt/moonshine")

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("STT · Moonshine (not loaded)")
    override val status: StateFlow<String> = _status.asStateFlow()

    @Volatile private var recognizer: OfflineRecognizer? = null
    private val initLock = Mutex()

    fun modelPresent(): Boolean =
        REQUIRED_FILES.all { File(modelDir, it).exists() }

    /** Load the model if its files are present. No-op once ready; never throws. */
    override fun init() {
        if (recognizer != null) return
        if (!modelPresent()) {
            _status.value = "STT · Moonshine (import model via Settings)"
            return
        }
        try {
            val d = modelDir.absolutePath
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = "$d/preprocess.onnx",
                        encoder = "$d/encode.onnx",
                        uncachedDecoder = "$d/uncached_decode.onnx",
                        cachedDecoder = "$d/cached_decode.onnx",
                    ),
                    tokens = "$d/tokens.txt",
                    numThreads = 2,
                    debug = false,
                ),
            )
            recognizer = OfflineRecognizer(config = config)
            _ready.value = true
            _status.value = "STT · Moonshine (ready)"
            Log.i(TAG, "Moonshine STT loaded from $d")
        } catch (e: Throwable) {
            Log.e(TAG, "Moonshine init failed", e)
            _status.value = "STT · Moonshine (load failed: ${e.message})"
        }
    }

    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): String =
        withContext(Dispatchers.IO) {
            if (pcm.isEmpty()) return@withContext ""
            initLock.withLock { if (recognizer == null) init() }
            val rec = recognizer ?: return@withContext ""
            try {
                val samples = FloatArray(pcm.size) { i -> pcm[i] / 32768f }
                val stream = rec.createStream()
                try {
                    stream.acceptWaveform(samples, sampleRate)
                    rec.decode(stream)
                    rec.getResult(stream).text.trim()
                } finally {
                    stream.release()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Moonshine transcribe failed", e)
                ""
            }
        }

    override fun close() {
        runCatching { recognizer?.release() }
        recognizer = null
        _ready.value = false
        _status.value = "STT · Moonshine (not loaded)"
    }

    companion object {
        const val TAG = "MoonshineStt"
        val REQUIRED_FILES = listOf(
            "preprocess.onnx", "encode.onnx",
            "uncached_decode.onnx", "cached_decode.onnx", "tokens.txt",
        )
    }
}
