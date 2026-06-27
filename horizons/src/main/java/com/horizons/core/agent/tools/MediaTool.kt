package com.horizons.core.agent.tools

import android.content.Context
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.view.KeyEvent
import com.horizons.core.agent.ToolResult
import org.json.JSONObject

class MediaTool(private val context: Context) {

    fun control(action: String): ToolResult {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return ToolResult("media", false, "MediaSessionManager unavailable")

        return try {
            val controllers = msm.getActiveSessions(null)
            if (controllers.isEmpty()) return ToolResult("media", true, """{"status":"no active media session"}""")

            val active = controllers.firstOrNull { it.playbackState != null } ?: controllers.first()

            when (action.lowercase()) {
                "status" -> {
                    val state = active.playbackState
                    val meta  = active.metadata
                    val obj   = JSONObject().apply {
                        put("player",  active.packageName)
                        put("state",   stateLabel(state?.state))
                        put("title",   meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "")
                        put("artist",  meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "")
                        put("album",   meta?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: "")
                    }
                    ToolResult("media", true, obj.toString())
                }
                "play"  -> { active.transportControls.play();            ToolResult("media", true, "Play") }
                "pause" -> { active.transportControls.pause();           ToolResult("media", true, "Paused") }
                "next"  -> { active.transportControls.skipToNext();      ToolResult("media", true, "Next track") }
                "prev"  -> { active.transportControls.skipToPrevious();  ToolResult("media", true, "Previous track") }
                else    -> ToolResult("media", false, "Unknown action: $action. Use play|pause|next|prev|status")
            }
        } catch (e: SecurityException) {
            ToolResult("media", false, "Notification access required — enable Horizons in Settings → Notification access")
        } catch (e: Exception) {
            ToolResult("media", false, e.message ?: "media control failed")
        }
    }

    private fun stateLabel(state: Int?): String = when (state) {
        PlaybackState.STATE_PLAYING  -> "playing"
        PlaybackState.STATE_PAUSED   -> "paused"
        PlaybackState.STATE_STOPPED  -> "stopped"
        PlaybackState.STATE_BUFFERING -> "buffering"
        null -> "unknown"
        else -> "state_$state"
    }
}
