package com.horizons.provider

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val systemPromptOverride: String = "",
    val cacheTtl: String = "1h",
    val debugLogLevel: Int = 0,
    val defaultBackendId: String = "",
)

class SettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<AppSettings> = _snapshot.asStateFlow()

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_snapshot.value)
        _snapshot.value = next
        persist(next)
    }

    private fun load() = AppSettings(
        systemPromptOverride = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: "",
        cacheTtl = prefs.getString(KEY_CACHE_TTL, "1h") ?: "1h",
        debugLogLevel = prefs.getInt(KEY_LOG_LEVEL, 0),
        defaultBackendId = prefs.getString(KEY_DEFAULT_BACKEND, "") ?: "",
    )

    private fun persist(s: AppSettings) {
        prefs.edit()
            .putString(KEY_SYSTEM_PROMPT, s.systemPromptOverride)
            .putString(KEY_CACHE_TTL, s.cacheTtl)
            .putInt(KEY_LOG_LEVEL, s.debugLogLevel)
            .putString(KEY_DEFAULT_BACKEND, s.defaultBackendId)
            .apply()
    }

    private companion object {
        const val KEY_SYSTEM_PROMPT = "systemPromptOverride"
        const val KEY_CACHE_TTL = "cacheTtl"
        const val KEY_LOG_LEVEL = "debugLogLevel"
        const val KEY_DEFAULT_BACKEND = "defaultBackendId"
    }
}
