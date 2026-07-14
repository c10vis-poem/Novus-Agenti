package com.horizons.audio

import android.content.Context

/**
 * Creates the best available [VadDetector] for the current device.
 *
 * Prefers [SileroVadDetector] when its ONNX model is available anywhere in
 * its resolution chain (assets → filesDir/models → /Download — drop
 * `silero_vad.onnx` in via ModelImportActivity or Downloads, no rebuild);
 * otherwise falls back to [RmsVadDetector] (energy gate) so the voice loop
 * always works.
 */
object VadFactory {
    fun create(context: Context): VadDetector {
        return if (SileroVadDetector.modelAvailable(context)) {
            SileroVadDetector(context)
        } else {
            RmsVadDetector()
        }
    }
}
