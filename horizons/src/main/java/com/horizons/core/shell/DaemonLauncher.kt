package com.horizons.core.shell

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Launches the native ONNX-RT/QNN engine binary as a detached background daemon.
 *
 * Expected binary location: context.filesDir/ort_engine
 * Deploy via: adb push ort_engine /data/data/com.horizons/files/ort_engine
 *             adb shell chmod +x /data/data/com.horizons/files/ort_engine
 *
 * The daemon serves inference requests at 127.0.0.1:8080 (see NpuClient).
 *
 * Priority: If root is available, oom_score_adj is set to -1000 (unkillable by lmkd).
 * Without root, the daemon inherits the app's oom_score_adj and may be killed under
 * extreme memory pressure. The ADB shell -950 loophole documented in ADB_CLOUD_BUILD
 * requires Wireless Debugging; this launcher uses sh -T- detach instead.
 */
class DaemonLauncher(
    private val context: Context,
    private val binaryName: String = ENGINE_BINARY,
) {

    data class DaemonHandle(val pid: Int, val logPath: String)

    /**
     * Preferred: the daemon packaged inside the APK as jniLibs/arm64-v8a/libort_engine.so.
     * useLegacyPackaging=true extracts it to nativeLibraryDir at install time, which is
     * the only location a targetSdk 29+ app may exec() from — SELinux denies exec on
     * app-writable storage (filesDir), so the old adb-push path can never actually run
     * on Android 10+. The filesDir fallback is kept only for rooted/legacy sideloads.
     */
    private fun resolveEngineFile(): File {
        val packagedName =
            if (binaryName == LLAMA_BINARY) PACKAGED_LLAMA_LIB else PACKAGED_ENGINE_LIB
        val packaged = File(context.applicationInfo.nativeLibraryDir, packagedName)
        if (packaged.exists()) return packaged
        return File(context.filesDir, binaryName)
    }

    suspend fun launch(engineArgs: List<String> = emptyList()): Result<DaemonHandle> =
        withContext(Dispatchers.IO) {
            val engine = resolveEngineFile()
            if (!engine.exists()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Engine binary not found. Looked in:\n" +
                        "  ${context.applicationInfo.nativeLibraryDir}/$PACKAGED_ENGINE_LIB (APK-packaged)\n" +
                        "  ${File(context.filesDir, binaryName).absolutePath} (legacy)"
                    )
                )
            }
            // nativeLibraryDir is a read-only fs — only chmod the legacy filesDir copy.
            if (!engine.canExecute() && engine.parentFile == context.filesDir) {
                engine.setExecutable(true)
            }

            val logFile = File(context.getExternalFilesDir(null), "$binaryName.log")

            // libonnxruntime.so ships in the APK's native lib dir (ORT maven dep);
            // filesDir is the legacy location for manually imported runtime files.
            val libDir = "${context.applicationInfo.nativeLibraryDir}:${context.filesDir.absolutePath}"

            // mksh -T- detaches the child from the controlling tty, reparenting it
            // to init so it survives shell exit. Equivalent to nohup + setsid.
            // llama-server's ggml-hexagon backend reads the NPU session count and
            // DSP skel search path from its environment (wiki/NPU-RUNTIME-PATHS.md Path 2).
            val envPrefix = if (binaryName == LLAMA_BINARY)
                "GGML_HEXAGON_NDEV=2 DSP_LIBRARY_PATH=${context.applicationInfo.nativeLibraryDir}:${context.filesDir.absolutePath} " else ""
            val args = engineArgs.joinToString(" ")
            val shellCmd = "${envPrefix}LD_LIBRARY_PATH=$libDir ${engine.absolutePath} $args >> ${logFile.absolutePath} 2>&1 &"

            return@withContext try {
                val proc = ProcessBuilder("/system/bin/sh", "-T-", "-c", shellCmd)
                    .redirectErrorStream(true)
                    .start()
                proc.waitFor() // shell exits immediately; daemon lives on under init

                val pid = resolveEnginePid(engine.name)
                if (pid > 0) {
                    applyOomImmunity(pid)
                    Log.i(TAG, "Engine daemon PID=$pid log=${logFile.absolutePath}")
                    Result.success(DaemonHandle(pid, logFile.absolutePath))
                } else {
                    Result.failure(RuntimeException("Engine launched but PID resolution failed — check ${logFile.absolutePath}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun isRunning(): Boolean = resolveEnginePid(resolveEngineFile().name) > 0

    fun stop() {
        val pid = resolveEnginePid(resolveEngineFile().name)
        if (pid > 0) {
            try { Runtime.getRuntime().exec(arrayOf("kill", "-15", pid.toString())).waitFor() }
            catch (_: Exception) {}
            Log.i(TAG, "Sent SIGTERM to engine PID=$pid")
        }
    }

    private fun resolveEnginePid(binaryName: String): Int {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("pidof", binaryName))
            proc.inputStream.bufferedReader().readLine()
                ?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull() ?: -1
        } catch (_: Exception) { -1 }
    }

    private fun applyOomImmunity(pid: Int) {
        // Root only. Silent no-op without root — caller does not need to handle failure.
        try {
            Runtime.getRuntime()
                .exec(arrayOf("su", "-c", "echo -1000 > /proc/$pid/oom_score_adj"))
                .waitFor()
            Log.i(TAG, "oom_score_adj set to -1000 for PID=$pid")
        } catch (_: Exception) {
            Log.d(TAG, "oom_score_adj: root not available; daemon runs at app priority")
        }
    }

    companion object {
        private const val TAG    = "DaemonLauncher"
        const val ENGINE_BINARY  = "ort_engine"
        /** APK-packaged daemon: jniLibs/arm64-v8a/libort_engine.so (CI copies it in). */
        const val PACKAGED_ENGINE_LIB = "libort_engine.so"
        /** GGUF runtime family: llama.cpp llama-server (+ ggml-hexagon NPU backend). */
        const val LLAMA_BINARY = "llama-server"
        /** jniLibs name for the llama daemon when CI packages it (same exec rule as ort_engine). */
        const val PACKAGED_LLAMA_LIB = "libllama_server.so"
        const val ENGINE_PORT    = 8080

        /** Runtime-family dispatch: GGUF models run on llama-server, everything else on ort_engine. */
        fun familyFor(modelPath: String): String =
            if (modelPath.lowercase().endsWith(".gguf")) LLAMA_BINARY else ENGINE_BINARY
    }
}
