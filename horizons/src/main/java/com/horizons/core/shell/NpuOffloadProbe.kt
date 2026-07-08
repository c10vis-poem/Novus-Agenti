package com.horizons.core.shell

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile

/**
 * Reads the tail of the llama-server log and answers the only question that
 * matters for the acceptance bar: is decode actually running on the Hexagon
 * NPU, or did it silently fall back to CPU?
 *
 * Detection is deliberately loose-but-cheap string matching:
 *  - hexagon backend init lines carry "ggml-hex" / "hexagon" / "HTPn"
 *  - llama.cpp's loader prints "offloaded X/Y layers" regardless of backend
 * Both signals together = NPU active with a measurable offload ratio.
 * Hexagon markers absent while the daemon is healthy = CPU-only fallback,
 * which is exactly the silent failure mode sessions 17-18 fought.
 */
object NpuOffloadProbe {

    private const val TAG = "NpuOffloadProbe"
    private const val STATUS_FILE = "npu-status.json"
    private const val TAIL_BYTES = 128 * 1024L

    private val HEXAGON_MARKER = Regex("""(?i)ggml-hex|hexagon|\bHTP\d""")
    private val OFFLOAD_LINE = Regex("""offloaded\s+(\d+)\s*/\s*(\d+)\s+layers""")

    data class Status(
        val hexagonDetected: Boolean,
        val layersOffloaded: Int,
        val layersTotal: Int,
        val summary: String,
    )

    fun probe(logFile: File): Status {
        val tail = readTail(logFile)
        if (tail.isBlank()) {
            return Status(false, 0, 0, "no daemon log yet")
        }
        val hexagon = HEXAGON_MARKER.containsMatchIn(tail)
        val offload = OFFLOAD_LINE.findAll(tail).lastOrNull()
        val offloaded = offload?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = offload?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val summary = when {
            hexagon && total > 0 -> "NPU: Hexagon active — $offloaded/$total layers offloaded"
            hexagon -> "NPU: Hexagon backend initialized"
            total > 0 -> "⚠ CPU FALLBACK — $offloaded/$total layers offloaded, no Hexagon init in log"
            else -> "⚠ CPU FALLBACK suspected — no Hexagon markers in daemon log"
        }
        return Status(hexagon, offloaded, total, summary)
    }

    /** Persist for the main-process UI (RouterPane reads this file). */
    fun writeStatus(context: Context, status: Status) {
        try {
            File(context.filesDir, STATUS_FILE).writeText(
                JSONObject()
                    .put("hexagonDetected", status.hexagonDetected)
                    .put("layersOffloaded", status.layersOffloaded)
                    .put("layersTotal", status.layersTotal)
                    .put("summary", status.summary)
                    .put("updatedAt", System.currentTimeMillis())
                    .toString()
            )
        } catch (e: Exception) {
            Log.w(TAG, "failed to persist NPU status", e)
        }
    }

    fun readStatus(context: Context): Status? {
        val f = File(context.filesDir, STATUS_FILE)
        if (!f.exists()) return null
        return try {
            val j = JSONObject(f.readText())
            Status(
                hexagonDetected = j.optBoolean("hexagonDetected", false),
                layersOffloaded = j.optInt("layersOffloaded", 0),
                layersTotal = j.optInt("layersTotal", 0),
                summary = j.optString("summary", ""),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readTail(logFile: File): String {
        if (!logFile.exists() || logFile.length() == 0L) return ""
        return try {
            RandomAccessFile(logFile, "r").use { raf ->
                val start = (raf.length() - TAIL_BYTES).coerceAtLeast(0)
                raf.seek(start)
                val buf = ByteArray((raf.length() - start).toInt())
                raf.readFully(buf)
                String(buf, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to read daemon log tail", e)
            ""
        }
    }
}
