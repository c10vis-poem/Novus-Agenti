package com.horizons.core.tts

import android.util.Log
import com.horizons.core.state.AppStateStore
import com.horizons.core.stt.DaemonSttClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * TTS via the media **daemon** — the sibling of DaemonSttClient.
 *
 * STT and TTS share one detached media-daemon process (Moonshine STT + Kokoro/
 * Sherpa-ONNX TTS), separate from the LLM+vision daemon (session-16 decision).
 * This is the wire-contract client only — the in-app SherpaOnnxTtsClient still
 * runs TTS in-process today; migrating callers to this client is a follow-up,
 * not done in this pass.
 *
 * Daemon contract (this client's side):
 *   POST {base}/tts   body: {"text":"…","voice":"…","speed":1.0}
 *                     → 200, body = audio/wav bytes
 *   GET  {base}/health                  → 200 when the daemon + model are ready
 *
 * Base URL defaults to the same media daemon as DaemonSttClient (127.0.0.1:8091) —
 * one process serves both /stt and /tts.
 */
class DaemonTtsClient(private val appState: AppStateStore) {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("TTS · media daemon (not connected)")
    val status: StateFlow<String> = _status.asStateFlow()

    private fun baseUrl(): String =
        appState.get(DaemonSttClient.KEY_MEDIA_DAEMON_URL)?.takeIf { it.isNotBlank() }
            ?: DaemonSttClient.DEFAULT_BASE

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
        _status.value = if (up) "TTS · media daemon (Kokoro)" else "TTS · media daemon (not connected)"
        up
    }

    /** Returns raw WAV bytes, or null on failure. Caller owns playback. */
    suspend fun synthesize(text: String, voice: String = "", speed: Float = 1.0f): ByteArray? =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext null
            try {
                val conn = URL("${baseUrl()}/tts").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 3_000
                conn.readTimeout = 30_000
                val body = JSONObject().apply {
                    put("text", text)
                    if (voice.isNotBlank()) put("voice", voice)
                    put("speed", speed)
                }.toString().toByteArray()
                conn.outputStream.use { it.write(body) }

                if (conn.responseCode !in 200..299) {
                    _ready.value = false
                    Log.w(TAG, "media daemon /tts HTTP ${conn.responseCode}")
                    conn.disconnect()
                    return@withContext null
                }
                val wav = conn.inputStream.readBytes()
                conn.disconnect()
                _ready.value = true
                wav
            } catch (e: Exception) {
                _ready.value = false
                _status.value = "TTS · media daemon (not connected)"
                Log.w(TAG, "media daemon /tts failed: ${e.message}")
                null
            }
        }

    companion object {
        const val TAG = "DaemonTtsClient"
    }
}
