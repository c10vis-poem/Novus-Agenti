package com.horizons.core.state

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for persisted app state — credentials, toggles,
 * picker selections.
 *
 * Backed by EncryptedSharedPreferences so credentials don't sit in plaintext.
 *
 * Construction MUST NOT crash the process: Keystore/keyset failures (transient
 * races, corrupted keysets after backup-restore) get one retry, then degrade
 * to an in-memory store (prefs == null) so the app boots without persisted
 * credentials instead of crash-looping.
 */
class AppStateStore(context: Context) {
    // null => encrypted storage unavailable this session; state is in-memory only.
    private val prefs: SharedPreferences? = createPrefs(context.applicationContext)

    private fun createPrefs(ctx: Context): SharedPreferences? {
        repeat(2) { attempt ->
            try {
                return EncryptedSharedPreferences.create(
                    ctx,
                    FILE,
                    MasterKey.Builder(ctx)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences init failed (attempt ${attempt + 1})", e)
                try { Thread.sleep(150) } catch (_: InterruptedException) { }
            }
        }
        Log.e(TAG, "Encrypted store unavailable — falling back to in-memory state")
        return null
    }

    private val _snapshot = MutableStateFlow(loadAll())
    val snapshot: StateFlow<Map<String, String>> = _snapshot.asStateFlow()

    fun get(key: String): String? = _snapshot.value[key]
    fun has(key: String): Boolean = !get(key).isNullOrBlank()

    fun put(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
        _snapshot.value = _snapshot.value + (key to value)
    }

    fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
        _snapshot.value = _snapshot.value - key
    }

    private fun loadAll(): Map<String, String> =
        prefs?.all?.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }?.toMap()
            ?: emptyMap()

    companion object {
        private const val TAG  = "AppStateStore"
        private const val FILE = "horizons_app_state"

        // Credentials
        const val KEY_HF_TOKEN          = "hf.token"
        const val KEY_GITHUB_TOKEN      = "github.token"
        const val KEY_LAST_SCREENSHOT   = "screen.last_path"

        // Router mode — "on-device" | "cloud" | "custom"
        const val KEY_ROUTER_MODE       = "router.mode"

        // Cloud API tokens (used by AgentLoop HttpFetch tool via bearer_token_key)
        const val KEY_API_SAMBANOVA     = "api.sambanova"
        const val KEY_API_OPENROUTER    = "api.openrouter"
        const val KEY_API_QAI_HUB       = "api.qai_hub"

        // TTS (Sherpa-ONNX / Kokoro)
        const val KEY_TTS_VOICE         = "tts.voice_id"
        const val KEY_TTS_SPEED         = "tts.speed"
    }
}
