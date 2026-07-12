package com.horizons.core.shell

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Extracts the ort_engine daemon binary and QNN runtime libs from APK assets to filesDir on first launch.
 *
 * Expected APK assets layout (absent until daemon is built and packaged):
 *   assets/ort_engine                — ONNX-RT + QNN EP daemon (Qwen3.5-9B, qnn_context_binary)
 *   assets/qnn/libQnnHtp.so          — Hexagon HTP execution provider
 *   assets/qnn/libQnnSystem.so       — QNN system library
 *   assets/qnn/libQnnHtpV79Skel.so   — HTP v79 skeleton for SM8750
 *
 * All extractions are silent no-ops until the binaries are packaged into APK assets.
 * isInstalled() returns false when no binary is present.
 */
object NativeBinaryInstaller {

    private val QNN_LIBS = listOf(
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnHtpV79Skel.so",
    )

    fun install(context: Context): Boolean {
        val ortOk = extractAsset(context, DaemonLauncher.ENGINE_BINARY, executable = true) &&
            QNN_LIBS.all { lib -> extractAsset(context, "qnn/$lib") }
        return ortOk
    }

    fun isInstalled(context: Context): Boolean =
        File(context.filesDir, DaemonLauncher.ENGINE_BINARY).canExecute()

    fun installedBinaryName(context: Context): String? =
        if (File(context.filesDir, DaemonLauncher.ENGINE_BINARY).canExecute())
            DaemonLauncher.ENGINE_BINARY
        else null

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
            Log.i(TAG, "Extracted $assetPath -> ${dest.absolutePath}")
            true
        }
    } catch (_: Exception) {
        false  // asset not packaged yet -- non-fatal until ort_engine is built
    }

    private const val TAG = "NativeBinaryInstaller"
}
