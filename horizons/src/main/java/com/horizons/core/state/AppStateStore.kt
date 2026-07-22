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
 * SELF-HEALING: EncryptedSharedPreferences throws on construction when the
 * Keystore master key and the encrypted prefs file fall out of sync — which
 * happens on app update, reinstall, or restore-from-backup, and used to hard-
 * crash the app at launch ("won't boot anymore"). If [resetOnCorruption] is
 * set (or a first strict attempt fails), the corrupt prefs file and stale key
 * are cleared and the store is rebuilt empty rather than throwing.
 */
class AppStateStore(context: Context, resetOnCorruption: Boolean = false) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext, resetOnCorruption)

    private val _snapshot = MutableStateFlow(loadAll())
    val snapshot: StateFlow<Map<String, String>> = _snapshot.asStateFlow()

    fun get(key: String): String? = _snapshot.value[key]
    fun has(key: String): Boolean = !get(key).isNullOrBlank()

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        _snapshot.value = _snapshot.value + (key to value)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
        _snapshot.value = _snapshot.value - key
    }

    private fun loadAll(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    companion object {
        private const val TAG = "AppStateStore"
        private const val FILE = "horizons_app_state"

        private fun buildEncrypted(ctx: Context): SharedPreferences =
            EncryptedSharedPreferences.create(
                ctx,
                FILE,
                MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        /**
         * Build the encrypted store, healing a corrupt key/file mismatch instead
         * of throwing. A decryption failure here is what turns an app update into
         * a boot loop; wiping the (unreadable-anyway) prefs recovers cleanly.
         */
        private fun createPrefs(ctx: Context, forceReset: Boolean): SharedPreferences {
            if (!forceReset) {
                try {
                    return buildEncrypted(ctx)
                } catch (e: Throwable) {
                    Log.e(TAG, "Encrypted prefs unreadable — resetting: ${e.message}")
                }
            }
            // Clear the corrupt encrypted file and the stale Keystore key, then rebuild.
            runCatching { ctx.deleteSharedPreferences(FILE) }
            runCatching {
                val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
            }
            return try {
                buildEncrypted(ctx)
            } catch (e: Throwable) {
                Log.e(TAG, "Encrypted prefs still failing after reset — using plaintext fallback: ${e.message}")
                // Last resort: a plain prefs file so the app is always usable.
                ctx.getSharedPreferences("${FILE}_plain", Context.MODE_PRIVATE)
            }
        }

        /** Default alias used by androidx.security MasterKey.Builder. */
        private const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS

        // Credentials
        const val KEY_HF_TOKEN          = "hf.token"
        const val KEY_GITHUB_TOKEN      = "github.token"
        const val KEY_LAST_SCREENSHOT   = "screen.last_path"

        // Router mode — "on-device" | "cloud" | "custom"
        const val KEY_ROUTER_MODE       = "router.mode"

        // Explicit user-pinned model file — the "plugged in" switch. Nothing
        // auto-loads a landed file until the user flips this in Monitor.
        const val KEY_ACTIVE_MODEL      = "runtime.active_model"

        // Cloud API tokens (used by AgentLoop HttpFetch tool via bearer_token_key)
        const val KEY_API_SAMBANOVA     = "api.sambanova"
        const val KEY_API_OPENROUTER    = "api.openrouter"
        const val KEY_API_QAI_HUB       = "api.qai_hub"

        // TTS (Sherpa-ONNX / Kokoro)
        const val KEY_TTS_VOICE         = "tts.voice_id"
        const val KEY_TTS_SPEED         = "tts.speed"
    }
}
