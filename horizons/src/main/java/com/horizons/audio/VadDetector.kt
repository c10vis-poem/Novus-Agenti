package com.horizons.audio

/**
 * Minimal interface for voice activity detection.
 *
 * Implementations: [RmsVadDetector] (energy-based, always available),
 * [SileroVadDetector] (ONNX model, requires assets/silero_vad.onnx).
 */
interface VadDetector {
    suspend fun isSpeech(pcmChunk: ShortArray, sampleRate: Int): Boolean
    fun close() {}
}
