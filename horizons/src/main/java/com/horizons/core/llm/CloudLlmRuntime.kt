package com.horizons.core.llm

import android.util.Log
import com.horizons.core.state.AppStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI-compatible chat completions runtime for cloud APIs.
 *
 * Works with: OpenRouter, SambaNova, HuggingFace Inference, any
 * endpoint that speaks POST /v1/chat/completions with SSE streaming.
 *
 * Configured entirely from AppStateStore — no hardcoded keys.
 */
class CloudLlmRuntime(private val appState: AppStateStore) : LlmRuntime {

    private val _backendStatus = MutableStateFlow("Cloud API · not configured")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    data class CloudConfig(
        val endpoint: String,
        val apiKey: String,
        val model: String,
        val label: String,
    )

    fun resolveConfig(): CloudConfig? {
        val customEndpoint = appState.get(KEY_CLOUD_ENDPOINT)?.takeIf { it.isNotBlank() }
        val customModel = appState.get(KEY_CLOUD_MODEL)?.takeIf { it.isNotBlank() }

        val openRouter = appState.get(AppStateStore.KEY_API_OPENROUTER)
        if (!openRouter.isNullOrBlank()) {
            return CloudConfig(
                endpoint = customEndpoint ?: "https://openrouter.ai/api/v1/chat/completions",
                apiKey = openRouter,
                model = customModel ?: "qwen/qwen-2.5-7b-instruct",
                label = "OpenRouter",
            )
        }
        val sambaNova = appState.get(AppStateStore.KEY_API_SAMBANOVA)
        if (!sambaNova.isNullOrBlank()) {
            return CloudConfig(
                endpoint = customEndpoint ?: "https://api.sambanova.ai/v1/chat/completions",
                apiKey = sambaNova,
                model = customModel ?: "Meta-Llama-3.1-8B-Instruct",
                label = "SambaNova",
            )
        }
        val hfToken = appState.get(AppStateStore.KEY_HF_TOKEN)
        if (!hfToken.isNullOrBlank() && customEndpoint != null) {
            return CloudConfig(
                endpoint = customEndpoint,
                apiKey = hfToken,
                model = customModel ?: "Qwen/Qwen2.5-7B-Instruct",
                label = "HuggingFace",
            )
        }
        return null
    }

    val isConfigured: Boolean get() = resolveConfig() != null

    fun refreshStatus() {
        val cfg = resolveConfig()
        _backendStatus.value = if (cfg != null) {
            "Adreno 830 · Cloud fallback (${cfg.label})"
        } else {
            "Cloud API · not configured"
        }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val cfg = resolveConfig()
        if (cfg == null) {
            emit("[No cloud API configured — add an API key in Settings]")
            return@flow
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content",
                "You are Novus Agenti, a capable AI assistant running on a Motorola Razr Ultra 2025. Be concise and helpful."))
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val body = JSONObject().apply {
            put("model", cfg.model)
            put("messages", messages)
            put("stream", true)
            put("max_tokens", 2048)
            put("temperature", 0.7)
        }

        val conn = URL(cfg.endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.setRequestProperty("Authorization", "Bearer ${cfg.apiKey}")
            conn.connectTimeout = 10_000
            conn.readTimeout = 120_000

            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText()?.take(500) ?: "HTTP ${conn.responseCode}"
                emit("[Cloud API error: $err]")
                return@flow
            }

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ") && line.length > 6) {
                        val data = line.substring(6).trim()
                        if (data == "[DONE]") break
                        try {
                            val delta = JSONObject(data)
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                            val content = delta.optString("content", "")
                            if (content.isNotEmpty()) emit(content)
                        } catch (_: Exception) { }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud stream error", e)
            emit("\n[Cloud stream error: ${e.message}]")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "CloudLlm"
        const val KEY_CLOUD_MODEL = "cloud.model"
        const val KEY_CLOUD_ENDPOINT = "cloud.endpoint"
    }
}
