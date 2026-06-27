package com.horizons.core.agent.tools

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.horizons.core.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalendarTool(private val context: Context) {

    fun readUpcoming(lookaheadDays: Int): ToolResult {
        val now  = System.currentTimeMillis()
        val end  = now + lookaheadDays.toLong() * 86_400_000L

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
        val selArgs   = arrayOf(now.toString(), end.toString())

        return try {
            val cursor = context.contentResolver.query(uri, projection, selection, selArgs, "${CalendarContract.Events.DTSTART} ASC")
                ?: return ToolResult("read_calendar", false, "ContentResolver returned null")
            val arr = JSONArray()
            cursor.use { c ->
                while (c.moveToNext()) {
                    arr.put(JSONObject().apply {
                        put("id",       c.getLong(0))
                        put("title",    c.getString(1) ?: "")
                        put("start",    c.getLong(2).toIso())
                        put("end",      c.getLong(3).toIso())
                        put("location", c.getString(4) ?: "")
                        put("desc",     c.getString(5) ?: "")
                    })
                }
            }
            ToolResult("read_calendar", true, arr.toString())
        } catch (e: SecurityException) {
            ToolResult("read_calendar", false, "Permission denied — grant READ_CALENDAR in Settings → App permissions")
        } catch (e: Exception) {
            ToolResult("read_calendar", false, e.message ?: "calendar read failed")
        }
    }

    fun createEvent(title: String, start: String, end: String, description: String, location: String): ToolResult {
        return try {
            val startMs = ZonedDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli()
            val endMs   = ZonedDateTime.parse(end,   DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli()

            val calId = defaultCalendarId() ?: return ToolResult("create_event", false, "No calendar found on device")

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID,   calId)
                put(CalendarContract.Events.TITLE,         title)
                put(CalendarContract.Events.DTSTART,       startMs)
                put(CalendarContract.Events.DTEND,         endMs)
                put(CalendarContract.Events.DESCRIPTION,   description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val insertUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return ToolResult("create_event", false, "Insert returned null")
            val eventId = ContentUris.parseId(insertUri)
            ToolResult("create_event", true, """{"event_id":$eventId,"title":"$title","start":"$start"}""")
        } catch (e: SecurityException) {
            ToolResult("create_event", false, "Permission denied — grant WRITE_CALENDAR in Settings → App permissions")
        } catch (e: Exception) {
            ToolResult("create_event", false, e.message ?: "event creation failed")
        }
    }

    private fun defaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        ) ?: return null
        var id: Long? = null
        cursor.use { c ->
            while (c.moveToNext()) {
                val cid = c.getLong(0)
                val primary = c.getInt(1)
                if (id == null) id = cid
                if (primary == 1) { id = cid; break }
            }
        }
        return id
    }

    private fun Long.toIso(): String =
        java.time.Instant.ofEpochMilli(this)
            .atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
