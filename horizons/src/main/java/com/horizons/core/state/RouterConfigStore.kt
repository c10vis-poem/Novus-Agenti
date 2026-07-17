package com.horizons.core.state

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ConfigStatus { READY, INCOMPLETE, RUNNING, SLEEPING, ARCHIVED }

data class SlotEntry(
    val slotName: String,
    val value: String,
    val filled: Boolean = value.isNotBlank(),
)

data class RouterConfig(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val name: String,
    val runtime: String = "",
    val backend: String = "",
    val model: String = "",
    val endpoint: String = "",
    val apiKeyRef: String = "",
    val customSlots: List<SlotEntry> = emptyList(),
    val status: ConfigStatus = ConfigStatus.INCOMPLETE,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val allSlots: List<SlotEntry>
        get() = buildList {
            if (runtime.isNotBlank()) add(SlotEntry("Runtime", runtime))
            else add(SlotEntry("Runtime", "", filled = false))
            if (backend.isNotBlank()) add(SlotEntry("Backend", backend))
            if (model.isNotBlank()) add(SlotEntry("Model", model))
            if (endpoint.isNotBlank()) add(SlotEntry("Endpoint", endpoint))
            if (apiKeyRef.isNotBlank()) add(SlotEntry("API Key", apiKeyRef))
            addAll(customSlots)
        }

    val missingSlots: List<String>
        get() = allSlots.filter { !it.filled }.map { it.slotName }

    val isReady: Boolean
        get() = runtime.isNotBlank() && missingSlots.isEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("runtime", runtime)
        put("backend", backend)
        put("model", model)
        put("endpoint", endpoint)
        put("apiKeyRef", apiKeyRef)
        put("status", status.name)
        put("createdAt", createdAt)
        put("customSlots", JSONArray().also { arr ->
            customSlots.forEach { s ->
                arr.put(JSONObject().apply {
                    put("slotName", s.slotName)
                    put("value", s.value)
                })
            }
        })
    }

    companion object {
        fun fromJson(j: JSONObject): RouterConfig = RouterConfig(
            id = j.optString("id", java.util.UUID.randomUUID().toString().take(8)),
            name = j.optString("name", "Unnamed"),
            runtime = j.optString("runtime", ""),
            backend = j.optString("backend", ""),
            model = j.optString("model", ""),
            endpoint = j.optString("endpoint", ""),
            apiKeyRef = j.optString("apiKeyRef", ""),
            status = try { ConfigStatus.valueOf(j.optString("status", "INCOMPLETE")) }
                     catch (_: Exception) { ConfigStatus.INCOMPLETE },
            createdAt = j.optLong("createdAt", System.currentTimeMillis()),
            customSlots = buildList {
                val arr = j.optJSONArray("customSlots")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        add(SlotEntry(s.optString("slotName"), s.optString("value")))
                    }
                }
            },
        )
    }
}

class RouterConfigStore(context: Context) {
    private val file = File(context.filesDir, "router_configs.json")
    private val _configs = MutableStateFlow<List<RouterConfig>>(emptyList())
    val configs: StateFlow<List<RouterConfig>> = _configs

    init { load() }

    private fun load() {
        if (!file.exists()) return
        try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<RouterConfig>()
            for (i in 0 until arr.length()) {
                list.add(RouterConfig.fromJson(arr.getJSONObject(i)))
            }
            _configs.value = list
        } catch (_: Exception) { }
    }

    private fun save() {
        val arr = JSONArray()
        _configs.value.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString(2))
    }

    fun add(config: RouterConfig) {
        _configs.value = _configs.value + config
        save()
    }

    fun update(id: String, transform: (RouterConfig) -> RouterConfig) {
        _configs.value = _configs.value.map { if (it.id == id) transform(it) else it }
        save()
    }

    fun remove(id: String) {
        _configs.value = _configs.value.filter { it.id != id }
        save()
    }

    fun get(id: String): RouterConfig? = _configs.value.find { it.id == id }

    fun setStatus(id: String, status: ConfigStatus) {
        update(id) { it.copy(status = status) }
    }
}
