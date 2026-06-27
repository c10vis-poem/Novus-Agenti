package com.horizons.core.agent.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.horizons.core.agent.ToolResult

class AlarmTool(private val context: Context) {

    fun setAlarm(time: String, label: String, days: List<String>): ToolResult {
        // Parse "HH:MM"
        val parts = time.split(":")
        if (parts.size != 2) return ToolResult("set_alarm", false, "Invalid time format. Use HH:MM.")
        val hour   = parts[0].trim().toIntOrNull() ?: return ToolResult("set_alarm", false, "Invalid hour")
        val minute = parts[1].trim().toIntOrNull() ?: return ToolResult("set_alarm", false, "Invalid minute")

        val dayInts = days.mapNotNull { DAY_MAP[it.lowercase().take(3)] }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
                if (dayInts.isNotEmpty()) putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, ArrayList(dayInts))
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult("set_alarm", true, "Alarm set for $time${if (label.isNotBlank()) " — $label" else ""}")
        } catch (e: Exception) {
            ToolResult("set_alarm", false, e.message ?: "AlarmClock intent failed")
        }
    }

    fun setTimer(durationSeconds: Int, label: String): ToolResult {
        if (durationSeconds <= 0) return ToolResult("set_timer", false, "duration_seconds must be > 0")
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            ToolResult("set_timer", true, "Timer set: ${if (mins > 0) "${mins}m " else ""}${secs}s${if (label.isNotBlank()) " — $label" else ""}")
        } catch (e: Exception) {
            ToolResult("set_timer", false, e.message ?: "Timer intent failed")
        }
    }

    private companion object {
        val DAY_MAP = mapOf(
            "sun" to 1, "mon" to 2, "tue" to 3, "wed" to 4,
            "thu" to 5, "fri" to 6, "sat" to 7,
        )
    }
}
