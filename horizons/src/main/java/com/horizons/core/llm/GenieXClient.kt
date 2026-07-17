package com.horizons.core.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * LlmRuntime backed by a local GenieX daemon (`geniex serve`) — the decided
 * primary runtime for Qwen3.5-9B (session 15, see wiki/GENIEX-DAEMON-PLAN.md).
 *
 * Wire protocol — standard OpenAI, documented in the GenieX README (stable,
 * not a guess):
 *   POST /v1/chat/completions  {"model":"…","messages":[…],"stream":true}
 *   → chunked SSE:             data: {"choices":[{"delta":{"content":"…"}}]}
 *                              data: [DONE]
 *   GET  /v1/models            → 200 + non-empty data[] once a model is
 *                                registered (replaces ort_engine's /health).
 *
 * Readiness mapping (keeps the serve-first / "alive ≠ ready" property the
 * watchdog depends on — see wiki/BOOT-SEQUENCE.md invariant I2):
 *   /v1/models 200 + ≥1 model  → ready
 *   port open, anything else   → loading (NEVER treated as dead)
 *   unreachable                → offline
 *
 * ADDITIVE, not yet the live runtime: CliffordService still launches/guards
 * ort_engine on :8080 and activates NpuClient. Switching activation to this
 * client is gated on reading the GenieX source (fork pending) for the exact
 * `geniex serve` flags (port/host/backend selection) and the standalone
 * arm64-android binary packaging — per GENIEX-DAEMON-PLAN.md's
 * audit-before-feature rule. The model id is discovered from /v1/models
 * rather than hardcoded, so flag details don't leak into this class.
 *
 * Vision: sent as an OpenAI image_url content part (data: URI, base64 JPEG) —
 * the standard multimodal shape. GenieX's QAIRT backend ships libgeniex_vlm;
 * verify the GGML route's VLM support against source when the fork lands.
 *
 * Think-token shim: <think>…</think> collapsed to one "[Thinking…]" emit,
 * same behavior as NpuClient.
 */
class GenieXClient(
    private val baseUrl: String = "http://127.0.0.1:$DEFAULT_PORT",
) : LlmRuntime {

    private val _backendStatus = MutableStateFlow("Hexagon HTP · NPU (geniex serve)")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    private val _perfMetrics = MutableStateFlow<LlmRuntime.PerfMetrics?>(null)
    override val perfMetrics: StateFlow<LlmRuntime.PerfMetrics?> = _perfMetrics.asStateFlow()

    /** Model id as registered by the daemon; discovered via /v1/models, cached. */
    @Volatile private var modelId: String? = null

    override fun stream(prompt: String): Flow<String> = timed(rawStream(prompt, null))

    /** Vision request — same daemon/socket as text (model+vision co-located). */
    override fun streamImage(jpeg: ByteArray, prompt: String): Flow<String> {
        val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
        val q = prompt.ifBlank { "What is on this screen?" }
        return timed(rawStream(q, b64))
    }

    private fun timed(source: Flow<String>): Flow<String> {
        val startNanos = System.nanoTime()
        var firstTokenNanos = -1L
        var tokenCount = 0
        return source.onEach {
            tokenCount++
            if (firstTokenNanos < 0) firstTokenNanos = System.nanoTime()
            val elapsedSec = (System.nanoTime() - firstTokenNanos) / 1_000_000_000.0
            _perfMetrics.value = LlmRuntime.PerfMetrics(
                firstTokenMs = (firstTokenNanos - startNanos) / 1_000_000,
                tokensPerSec = if (elapsedSec > 0) tokenCount / elapsedSec else 0.0,
                tokenCount = tokenCount,
            )
        }
    }

    private fun rawStream(prompt: String, imageB64: String?): Flow<String> = flow {
        val model = readyModelId()
        if (model == null) {
            when (probeCode()) {
                -1 -> emit("[GenieX daemon not reachable at ${baseUrl.removePrefix("http://")}]")
                else -> emit("[GenieX daemon is up but no model is registered yet — " +
                             "still loading, or `geniex pull` hasn't run. Try again shortly.]")
            }
            return@flow
        }

        val conn = URL("$baseUrl/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput      = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.connectTimeout = 5_000
            conn.readTimeout    = 120_000

            val content: Any = if (imageB64 == null) prompt else JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", prompt))
                put(JSONObject().put("type", "image_url").put("image_url",
                    JSONObject().put("url", "data:image/jpeg;base64,$imageB64")))
            }
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", content)))
                put("stream", true)
                put("max_tokens", 2048)
                put("temperature", 0.7)
            }.toString().toByteArray()
            conn.outputStream.use { it.write(body) }

            var inThink    = false
            var thinkShown = false

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (!line.startsWith("data: ") || line.length <= 6) {
                        line = reader.readLine(); continue
                    }
                    val payload = line.substring(6).trim()
                    if (payload == "[DONE]") break

                    val token = try {
                        JSONObject(payload).optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content", "") ?: ""
                    } catch (_: Exception) { "" }

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
            Log.e(TAG, "GenieX stream error", e)
            emit("\n[GenieX stream error: ${e.message}]")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Readiness + model discovery in one probe: 200 with a non-empty data[]
     * means ready, and the first model id is cached for chat requests.
     * Returns null while loading/offline — callers emit an honest message,
     * they never treat this as a dead daemon (invariant I2).
     */
    private fun readyModelId(): String? {
        modelId?.let { return it }
        try {
            val conn = URL("$baseUrl/v1/models").openConnection() as HttpURLConnection
            conn.connectTimeout = 1_000
            conn.readTimeout    = 2_000
            conn.requestMethod  = "GET"
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val first = JSONObject(text).optJSONArray("data")
                    ?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
                if (first != null) modelId = first
                return first
            }
            conn.disconnect()
        } catch (_: Exception) { /* offline/loading — fall through */ }
        return null
    }

    /** Raw probe for error messaging: HTTP code from /v1/models, -1 if unreachable. */
    private fun probeCode(): Int = try {
        val conn = URL("$baseUrl/v1/models").openConnection() as HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod  = "GET"
        val code = conn.responseCode
        conn.disconnect()
        code
    } catch (_: Exception) { -1 }

    companion object {
        private const val TAG = "GenieXClient"
        /** GenieX README-documented default (`geniex serve` → 127.0.0.1:18181/v1). */
        const val DEFAULT_PORT = 18181
    }
}
