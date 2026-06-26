package com.horizons.core.shell

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Extracts the inference daemon binary and runtime libs from APK assets to filesDir on first launch.
 *
 * Prefers genie_engine (Qualcomm Genie SDK, runs .bin QNN context binaries compiled by QAI Hub).
 * Falls back to ort_engine (ONNX Runtime + QNN EP, runs .dlc / ONNX models).
 *
 * Expected APK assets layout (absent until daemon is built and packaged):
 *   assets/genie_engine              — Genie SDK daemon (Qwen3-VL, Qwen2.5-VL, etc.)
 *   assets/genie/libGenie.so         — Qualcomm Genie runtime
 *   assets/genie/libQnnHtp.so        — Hexagon HTP execution provider
 *   assets/genie/libQnnSystem.so     — QNN system library
 *   assets/genie/libQnnHtpV75Skel.so — HTP v75 skeleton for SM8750
 *   assets/ort_engine                — ONNX-RT + QNN EP daemon (Gemma 4 QAT .dlc path)
 *   assets/qnn/libQnnHtp.so          — same libs for ort_engine path
 *   assets/qnn/libQnnSystem.so
 *   assets/qnn/libQnnHtpV75Skel.so
 *
 * All extractions are silent no-ops until the binaries are packaged into APK assets.
 * isInstalled() returns false when no binary is present.
 */
object NativeBinaryInstaller {

    private val GENIE_LIBS = listOf(
        "libGenie.so",
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnHtpV75Skel.so",
    )
    private val QNN_LIBS = listOf(
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnHtpV75Skel.so",
    )

    fun install(context: Context): Boolean {
        // Try Genie engine first; fall back to ort_engine.
        val genieOk = extractAsset(context, DaemonLauncher.GENIE_BINARY, executable = true) &&
            GENIE_LIBS.all { lib -> extractAsset(context, "genie/$lib") }
        if (genieOk) return true

        val ortOk = extractAsset(context, DaemonLauncher.ENGINE_BINARY, executable = true) &&
            QNN_LIBS.all { lib -> extractAsset(context, "qnn/$lib") }
        return ortOk
    }

    fun isInstalled(context: Context): Boolean =
        listOf(DaemonLauncher.GENIE_BINARY, DaemonLauncher.ENGINE_BINARY)
            .any { File(context.filesDir, it).canExecute() }

    fun installedBinaryName(context: Context): String? =
        listOf(DaemonLauncher.GENIE_BINARY, DaemonLauncher.ENGINE_BINARY)
            .firstOrNull { File(context.filesDir, it).canExecute() }

    private fun extractAsset(
        context: Context,
        assetPath: String,
        targetName: String = File(assetPath).name,
        executable: Boolean = false,
    ): Boolean = try {
        context.assets.open(assetPath).use { src ->
            val dest = File(context.filesDir, targetName)
            dest.outputStream().use { dst -> src.copyTo(dst) }
            if (executable) dest.setExecutable(true, true)
            Log.i(TAG, "Extracted $assetPath → ${dest.absolutePath}")
            true
        }
    } catch (_: Exception) {
        false  // asset not packaged yet — non-fatal until ort_engine is built
    }

    private const val TAG = "NativeBinaryInstaller"
}
