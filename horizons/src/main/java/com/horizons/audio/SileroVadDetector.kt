package com.horizons.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Silero VAD v4 detector backed by onnxruntime-android.
 *
 * Requires `assets/silero_vad.onnx` to be present; if the file is missing or
 * ORT fails to initialise, [disabled] is set to true and [isSpeech] always
 * returns false (letting the caller fall back gracefully).
 *
 * The Silero VAD v4 model accepts:
 *   - input  : float32[1, chunk]  (normalised PCM, values in [-1, 1])
 *   - sr     : int64[1]           (sample rate, typically 16000)
 *   - h / c  : float32[2, 1, 64] (LSTM state — zero-initialised, updated each call)
 *
 * Output:
 *   - output : float32[1, 1]      (speech probability)
 *   - hn / cn: float32[2, 1, 64] (updated LSTM state)
 *
 * For stateless, single-chunk inference (G15 scope) we reset LSTM state on
 * every call — sufficient for 20-30 ms energy gate at chunk boundaries.
 */
class SileroVadDetector(context: Context) : VadDetector {

    private val env: OrtEnvironment? = runCatching { OrtEnvironment.getEnvironment() }.getOrNull()
    private var session: OrtSession? = null
    var disabled: Boolean = false
        private set

    init {
        if (env == null) {
            Log.w(TAG, "OrtEnvironment unavailable — VAD disabled")
            disabled = true
        } else {
            try {
                val bytes = context.assets.open("silero_vad.onnx").use { it.readBytes() }
                session = env.createSession(bytes, OrtSession.SessionOptions())
                Log.i(TAG, "Silero VAD session created (${bytes.size} bytes)")
            } catch (e: Exception) {
                Log.w(TAG, "silero_vad.onnx missing or load failed: ${e.message} — VAD disabled")
                disabled = true
            }
        }
    }

    override suspend fun isSpeech(pcmChunk: ShortArray, sampleRate: Int): Boolean {
        if (disabled) return false
        val sess = session ?: return false
        val env = env ?: return false

        return try {
            // Normalise PCM16 → float32 in [-1, 1]
            val floats = FloatArray(pcmChunk.size) { i -> pcmChunk[i] / 32768f }

            // Build input tensors
            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(floats),
                longArrayOf(1L, floats.size.toLong())
            )
            val srTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(longArrayOf(sampleRate.toLong())),
                longArrayOf(1L)
            )
            // LSTM state: shape [2, 1, 64], zero-init for stateless per-chunk inference
            val stateZero = FloatArray(2 * 1 * 64) { 0f }
            val hTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(stateZero.copyOf()),
                longArrayOf(2L, 1L, 64L)
            )
            val cTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(stateZero.copyOf()),
                longArrayOf(2L, 1L, 64L)
            )

            val inputs = mapOf("input" to inputTensor, "sr" to srTensor, "h" to hTensor, "c" to cTensor)

            val result = sess.run(inputs)
            // Output tensor named "output", shape [1, 1]
            val output = result[0].value as Array<*>
            val prob = ((output[0] as FloatArray)[0])

            // Clean up
            inputTensor.close(); srTensor.close(); hTensor.close(); cTensor.close()
            result.close()

            prob > SPEECH_THRESHOLD
        } catch (e: Exception) {
            Log.e(TAG, "Silero inference error: ${e.message}", e)
            false
        }
    }

    override fun close() {
        runCatching { session?.close() }
        session = null
    }

    private companion object {
        const val TAG = "SileroVadDetector"
        const val SPEECH_THRESHOLD = 0.5f
    }
}
