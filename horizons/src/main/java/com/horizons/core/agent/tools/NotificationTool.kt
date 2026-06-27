package com.horizons.core.agent.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.horizons.core.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject

class NotificationTool(private val context: Context) {

    // ── Read (supplied by AgentNotificationListener) ──────────────────────────

    fun read(activeNotifications: List<StatusBarNotification>): ToolResult {
        val arr = JSONArray()
        activeNotifications.take(20).forEach { sbn ->
            val extras = sbn.notification.extras
            arr.put(JSONObject().apply {
                put("package", sbn.packageName)
                put("title",   extras.getString(android.app.Notification.EXTRA_TITLE) ?: "")
                put("text",    extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: "")
                put("time",    sbn.postTime)
            })
        }
        return ToolResult("read_notifications", true, arr.toString())
    }

    // ── Post ──────────────────────────────────────────────────────────────────

    fun post(title: String, body: String, channel: String): ToolResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm, channel)
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val id = (title + body).hashCode()
        nm.notify(id, notif)
        return ToolResult("post_notification", true, """{"notification_id":$id}""")
    }

    private fun ensureChannel(nm: NotificationManager, channelId: String) {
        if (nm.getNotificationChannel(channelId) != null) return
        val name = when (channelId) {
            "agent"  -> "Agent notifications"
            "alerts" -> "Alerts"
            else     -> channelId
        }
        nm.createNotificationChannel(
            NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
