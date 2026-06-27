package com.horizons.audio

import kotlin.math.sqrt

/**
 * Energy-based voice activity detector.
 *
 * Computes the RMS amplitude of the PCM chunk and returns true when it
 * exceeds [threshold].  No model file or native library required — this is
 * the PRIMARY [VadDetector] implementation and the build must succeed when
 * assets/silero_vad.onnx is absent.
 *
 * Default threshold of 600 works well for 16-bit PCM at 16 kHz captured via
 * [MediaRecorder.AudioSource.VOICE_RECOGNITION].
 */
class RmsVadDetector(private val threshold: Float = 600f) : VadDetector {

    override suspend fun isSpeech(pcmChunk: ShortArray, sampleRate: Int): Boolean {
        if (pcmChunk.isEmpty()) return false
        var sumSq = 0.0
        for (sample in pcmChunk) sumSq += sample.toDouble() * sample.toDouble()
        val rms = sqrt(sumSq / pcmChunk.size).toFloat()
        return rms > threshold
    }
}
