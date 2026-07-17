package com.horizons.core.diag

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uploads crash logs as GitHub issues so the operator's agent sessions can
 * read them directly — no on-device log spelunking (session-17 lesson: the
 * diag folder is unreachable from file managers/Termux on modern Android,
 * which made every crash a multi-round screenshot hunt).
 *
 * Runs from the CLIFFORD process (survives main-process death) on the CRS
 * tick. Requires the GitHub token already stored in Settings (KEY_GITHUB_TOKEN,
 * needs repo/issues scope). No token → silent no-op. Each crash.log content
 * hash is uploaded at most once (marker file), so a crash loop produces ONE
 * issue, not spam.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val REPO_ISSUES_URL =
        "https://api.github.com/repos/c10vis-poem/Novus-Agenti/issues"
    private const val MARKER = "crash_uploaded.marker"

    /** Call periodically (CRS tick). Cheap when there's nothing to do. */
    fun maybeUpload(ctx: Context) {
        val diagDir = ctx.getExternalFilesDir(null)?.let { File(it, "diag") } ?: return
        val crash = File(diagDir, "crash.log")
        if (!crash.canRead() || crash.length() == 0L) return

        // Token is read via a FRESH AppStateStore instance every time (only
        // reached when a crash actually exists, so the crypto setup cost is
        // rare). The caller runs in :clifford, whose long-lived AppStateStore
        // is a stale snapshot from process start — a token pasted into
        // Settings afterwards would be invisible to it forever. A fresh
        // instance re-reads the encrypted file from disk.
        val githubToken = runCatching {
            com.horizons.core.state.AppStateStore(ctx)
                .get(com.horizons.core.state.AppStateStore.KEY_GITHUB_TOKEN)
        }.getOrNull()
        if (githubToken.isNullOrBlank()) return

        val content = runCatching { crash.readText() }.getOrNull() ?: return
        val hash = content.hashCode().toString()
        val marker = File(diagDir, MARKER)
        if (marker.canRead() && marker.readText() == hash) return  // already reported

        val boot = File(diagDir, "boot.log")
            .takeIf { it.canRead() }
            ?.readLines()?.takeLast(60)?.joinToString("\n") ?: "(no boot.log)"

        val title = "[auto] on-device crash report (${android.os.Build.MODEL})"
        val body = buildString {
            append("Automated crash report from CLIFFORD.\n\n")
            append("## crash.log\n```\n")
            append(content.takeLast(6000))
            append("\n```\n\n## boot.log (tail)\n```\n")
            append(boot)
            append("\n```\n")
        }

        runCatching {
            val conn = URL(REPO_ISSUES_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $githubToken")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.outputStream.use {
                it.write(JSONObject().put("title", title).put("body", body)
                    .put("labels", org.json.JSONArray().put("crash-report"))
                    .toString().toByteArray())
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                marker.writeText(hash)
                Log.i(TAG, "crash report uploaded (HTTP $code)")
            } else {
                Log.w(TAG, "crash report upload failed: HTTP $code")
            }
        }.onFailure { Log.w(TAG, "crash report upload error: ${it.message}") }
    }
}
