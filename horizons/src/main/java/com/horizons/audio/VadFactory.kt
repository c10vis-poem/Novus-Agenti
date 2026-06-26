package com.horizons.audio

import android.content.Context

/**
 * Creates the best available [VadDetector] for the current device.
 *
 * Prefers [SileroVadDetector] (ONNX model) when `assets/silero_vad.onnx`
 * is present; otherwise falls back to [RmsVadDetector] so the build and
 * runtime always work without the model file.
 */
object VadFactory {
    fun create(context: Context): VadDetector {
        return try {
            context.assets.open("silero_vad.onnx").close()
            SileroVadDetector(context)
        } catch (_: Exception) {
            RmsVadDetector()
        }
    }
}
