package com.horizons.core.stt

import android.util.Log
import com.horizons.core.state.AppStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * STT via the media **daemon** — NOT in-process.
 *
 * The media daemon hosts STT (Moonshine small, CPU) and TTS (Kokoro/Sherpa-ONNX)
 * as one detached process, separate from the LLM+vision daemon (see NpuClient —
 * session-16 decision: vision shares the model's socket, STT/TTS do not). Keeping
 * STT/TTS out of the Kotlin UI process avoids OOM/crash risk in-app.
 * This class is just the STT client half; see DaemonTtsClient for the TTS half.
 * It ships 16-bit PCM (as WAV) to the daemon's `/stt` endpoint and returns the
 * transcript.
 *
 * Daemon contract (this client's side):
 *   POST {base}/stt   body: audio/wav   → 200, body = transcript text
 *   GET  {base}/health                  → 200 when the daemon + model are ready
 *
 * Base URL is configurable (default 127.0.0.1:8091); the model+vision daemon and
 * this media daemon are separate processes behind the same loopback socket layer.
 */
class DaemonSttClient(private val appState: AppStateStore) : SttEngine {

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("STT · media daemon (not connected)")
    override val status: StateFlow<String> = _status.asStateFlow()

    private fun baseUrl(): String =
        appState.get(KEY_MEDIA_DAEMON_URL)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE

    /** Probe the daemon's health so [ready] reflects reachability. Non-blocking-safe. */
    override fun init() { /* readiness is established lazily via probe()/transcribe() */ }

    suspend fun probe(): Boolean = withContext(Dispatchers.IO) {
        val up = try {
            val conn = URL("${baseUrl()}/health").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1_500
            conn.readTimeout = 1_500
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (_: Exception) { false }
        _ready.value = up
        _status.value = if (up) "STT · media daemon (Whisper)" else "STT · media daemon (not connected)"
        up
    }

    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): String =
        withContext(Dispatchers.IO) {
            if (pcm.isEmpty()) return@withContext ""
            val wav = pcmToWav(pcm, sampleRate)
            try {
                val conn = URL("${baseUrl()}/stt").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "audio/wav")
                conn.connectTimeout = 3_000
                conn.readTimeout = 30_000
                conn.outputStream.use { it.write(wav) }

                if (conn.responseCode !in 200..299) {
                    _ready.value = false
                    Log.w(TAG, "media daemon /stt HTTP ${conn.responseCode}")
                    conn.disconnect()
                    return@withContext ""
                }
                val body = conn.inputStream.bufferedReader().readText().trim()
                conn.disconnect()
                _ready.value = true
                // Accept either plain text or {"text": "..."}.
                parseTranscript(body)
            } catch (e: Exception) {
                _ready.value = false
                _status.value = "STT · media daemon (not connected)"
                Log.w(TAG, "media daemon /stt failed: ${e.message}")
                ""
            }
        }

    override fun close() { _ready.value = false }

    private fun parseTranscript(body: String): String =
        if (body.startsWith("{")) {
            runCatching { JSONObject(body).optString("text", "") }.getOrDefault("").trim()
        } else body.trim()

    private fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val dataBytes = pcm.size * 2
        val out = ByteArrayOutputStream(44 + dataBytes)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2)
            putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes)
        }.array()
        out.write(header)
        val buf = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) buf.putShort(s)
        out.write(buf.array())
        return out.toByteArray()
    }

    companion object {
        const val TAG = "DaemonSttClient"
        const val DEFAULT_BASE = "http://127.0.0.1:8091"
        const val KEY_MEDIA_DAEMON_URL = "media.daemon.url"
    }
}
