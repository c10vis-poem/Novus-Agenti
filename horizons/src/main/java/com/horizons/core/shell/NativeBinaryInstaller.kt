package com.horizons.core.shell

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Checks for the presence of native daemon binaries (ort_engine or llama-server)
 * and their runtime libraries.
 *
 * Primary path: binaries packaged as jniLibs/arm64-v8a/lib*.so, extracted to
 * nativeLibraryDir at install time — the ONLY exec()-allowed location on
 * targetSdk 29+ (SELinux denies exec on app-writable storage).
 *
 * Legacy path: assets extracted to filesDir (rooted/sideload only).
 *
 * isInstalled() returns false when no binary is present.
 */
object NativeBinaryInstaller {

    private val QNN_LIBS = listOf(
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnHtpV75Skel.so",
    )

    private val LLAMA_LIBS = listOf(
        "libggml-hexagon.so",
        "libggml-htp-v75.so",
        "libggml-htp-v79.so",
    )

    fun install(context: Context): Boolean {
        val ortOk = extractAsset(context, DaemonLauncher.ENGINE_BINARY, executable = true) &&
            QNN_LIBS.all { lib -> extractAsset(context, "qnn/$lib") }
        return ortOk
    }

    fun isInstalled(context: Context): Boolean {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        return File(nativeDir, DaemonLauncher.PACKAGED_ENGINE_LIB).exists() ||
            File(nativeDir, DaemonLauncher.PACKAGED_LLAMA_LIB).exists() ||
            File(context.filesDir, DaemonLauncher.ENGINE_BINARY).canExecute() ||
            File(context.filesDir, DaemonLauncher.LLAMA_BINARY).canExecute()
    }

    fun installedBinaryName(context: Context): String? {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        return when {
            File(nativeDir, DaemonLauncher.PACKAGED_LLAMA_LIB).exists() ->
                DaemonLauncher.PACKAGED_LLAMA_LIB
            File(nativeDir, DaemonLauncher.PACKAGED_ENGINE_LIB).exists() ->
                DaemonLauncher.PACKAGED_ENGINE_LIB
            File(context.filesDir, DaemonLauncher.LLAMA_BINARY).canExecute() ->
                DaemonLauncher.LLAMA_BINARY
            File(context.filesDir, DaemonLauncher.ENGINE_BINARY).canExecute() ->
                DaemonLauncher.ENGINE_BINARY
            else -> null
        }
    }

    fun hasLlamaDeps(context: Context): Boolean {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        return LLAMA_LIBS.all { File(nativeDir, it).exists() } ||
            LLAMA_LIBS.all { File(context.filesDir, it).exists() }
    }

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
