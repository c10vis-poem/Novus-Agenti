package com.horizons.watchdog.kag

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

// Local NDJSON ring buffer is the source of truth on-device; Jetson push
// is best-effort over WebSocket and lives off the hot path.
object KagEmitter {
    private const val TAG = "KagEmitter"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun emit(context: Context, event: KagEvent) {
        try {
            val dir = File(context.filesDir, "kag").apply { if (!exists()) mkdirs() }
            val file = File(dir, "events.ndjson")
            val line = json.encodeToString(event) + "\n"
            FileOutputStream(file, true).use { it.write(line.toByteArray(Charsets.UTF_8)) }
            pushToJetson(line)
        } catch (t: Throwable) {
            Log.w(TAG, "emit failed", t)
        }
    }

    // TODO: stand up org.java_websocket.client.WebSocketClient against the
    // Jetson endpoint and stream NDJSON lines. Stubbed until layer 6 lands.
    @Suppress("UNUSED_PARAMETER")
    private fun pushToJetson(ndjsonLine: String) {
        // no-op
    }
}
