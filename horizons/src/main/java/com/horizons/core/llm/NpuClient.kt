package com.horizons.core.llm

import android.util.Log
import com.horizons.core.shell.DaemonLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * LlmRuntime backed by the local ONNX-RT/QNN native engine daemon.
 *
 * The daemon (ort_engine) must be running on 127.0.0.1:DaemonLauncher.ENGINE_PORT.
 * Launch via DaemonLauncher. Install binary via NativeBinaryInstaller.
 *
 * Wire protocol:
 *   POST /api/v1/generate   {"prompt":"…","temperature":0.7,"max_tokens":2048,"stream":true}
 *   → chunked SSE:          data: {"token":"…","index":N}
 *   GET  /health            → 200 when ready
 *
 * Think-token shim: <think>…</think> blocks are collapsed to a single "[Thinking…]" emit.
 * The daemon may suppress them natively in future; this shim is a Kotlin-side safety net.
 */
class NpuClient : LlmRuntime {

    private val _backendStatus = MutableStateFlow("Hexagon HTP · NPU (ort_engine daemon)")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    override fun stream(prompt: String): Flow<String> = flow {
        if (!isDaemonReachable()) {
            emit("[NPU daemon not reachable at 127.0.0.1:${DaemonLauncher.ENGINE_PORT}]")
            return@flow
        }

        val conn = URL("http://127.0.0.1:${DaemonLauncher.ENGINE_PORT}/api/v1/generate")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput      = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.connectTimeout = 5_000
            conn.readTimeout    = 120_000

            val body = JSONObject().apply {
                put("prompt",      prompt)
                put("temperature", 0.7)
                put("max_tokens",  2048)
                put("stream",      true)
            }.toString().toByteArray()
            conn.outputStream.use { it.write(body) }

            var inThink    = false
            var thinkShown = false

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val token = if (line.startsWith("data: ") && line.length > 6) {
                        try { JSONObject(line.substring(6)).optString("token", "") }
                        catch (_: Exception) { line.substring(6) }
                    } else {
                        line = reader.readLine(); continue
                    }

                    if (token.isEmpty()) { line = reader.readLine(); continue }

                    when {
                        !inThink && "<think>" in token -> {
                            val before = token.substringBefore("<think>")
                            if (before.isNotEmpty()) emit(before)
                            inThink    = true
                            thinkShown = false
                        }
                        inThink && "</think>" in token -> {
                            inThink = false
                            val after = token.substringAfter("</think>")
                            if (after.isNotEmpty()) emit(after)
                        }
                        inThink -> {
                            if (!thinkShown) { emit("[Thinking…]"); thinkShown = true }
                        }
                        else -> emit(token)
                    }

                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "NPU stream error", e)
            emit("\n[NPU stream error: ${e.message}]")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun isDaemonReachable(): Boolean = try {
        val conn = URL("http://127.0.0.1:${DaemonLauncher.ENGINE_PORT}/health")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod  = "GET"
        val code = conn.responseCode
        conn.disconnect()
        code == 200
    } catch (_: Exception) { false }

    companion object { private const val TAG = "NpuClient" }
}
