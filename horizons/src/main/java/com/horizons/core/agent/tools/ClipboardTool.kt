package com.horizons.core.agent.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.horizons.core.agent.ToolResult

class ClipboardTool(private val context: Context) {

    fun read(): ToolResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
            ?: return ToolResult("read_clipboard", true, """{"text":""}""")
        return ToolResult("read_clipboard", true, """{"text":${org.json.JSONObject.quote(text)}}""")
    }

    fun write(text: String): ToolResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("agent", text))
        return ToolResult("write_clipboard", true, "Clipboard updated (${text.length} chars)")
    }
}
