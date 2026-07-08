package com.horizons.core.shell

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Hexagon/llama-server tuning knobs, persisted as a plain JSON file so both
 * the main process (RouterPane edits) and the :clifford process (daemon
 * launch) see the same values — SharedPreferences is not multi-process safe.
 *
 * Every knob maps 1:1 onto something the daemon reads at launch, so changes
 * take effect on the next daemon (re)start — no APK rebuild per experiment.
 */
data class NpuTuning(
    /** GGML_HEXAGON_NDEV — HTP sessions the model is split across. The 9B
     *  Q4_0 needs ≥2 to stay under the 4GB/session 32-bit cDSP ceiling. */
    val ndev: Int = 2,
    /** -c context size. KV cache is the RAM runaway on mobile. */
    val ctxSize: Int = 4096,
    /** -fa flash attention — memory-bandwidth win, keep on unless debugging. */
    val flashAttention: Boolean = true,
    /** GGML_HEXAGON_VERBOSE=1 — hexagon backend logs device/op placement.
     *  Turn on when diagnosing CPU fallback, off for daily use (log volume). */
    val verboseHexagon: Boolean = false,
    /** Free-form extra environment, e.g. "GGML_HEXAGON_OPMASK=0x3".
     *  Space-separated NAME=VALUE pairs, prepended to the daemon env. */
    val extraEnv: String = "",
    /** Free-form extra llama-server args appended after the built-ins. */
    val extraArgs: String = "",
) {
    fun save(context: Context) {
        val json = JSONObject()
            .put("ndev", ndev)
            .put("ctxSize", ctxSize)
            .put("flashAttention", flashAttention)
            .put("verboseHexagon", verboseHexagon)
            .put("extraEnv", extraEnv)
            .put("extraArgs", extraArgs)
        val target = file(context)
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(json.toString(2))
        if (!tmp.renameTo(target)) {
            // rename can fail across some vendor fs setups — fall back to direct write
            target.writeText(json.toString(2))
            tmp.delete()
        }
    }

    companion object {
        private const val TAG = "NpuTuning"
        private const val FILE_NAME = "npu-tuning.json"

        private fun file(context: Context) = File(context.filesDir, FILE_NAME)

        fun load(context: Context): NpuTuning {
            val f = file(context)
            if (!f.exists()) return NpuTuning()
            return try {
                val j = JSONObject(f.readText())
                NpuTuning(
                    ndev = j.optInt("ndev", 2).coerceIn(1, 8),
                    ctxSize = j.optInt("ctxSize", 4096).coerceIn(512, 32768),
                    flashAttention = j.optBoolean("flashAttention", true),
                    verboseHexagon = j.optBoolean("verboseHexagon", false),
                    extraEnv = j.optString("extraEnv", ""),
                    extraArgs = j.optString("extraArgs", ""),
                )
            } catch (e: Exception) {
                Log.w(TAG, "npu-tuning.json unreadable — using defaults", e)
                NpuTuning()
            }
        }
    }
}
