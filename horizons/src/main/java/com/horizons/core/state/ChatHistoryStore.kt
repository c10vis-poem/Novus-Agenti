package com.horizons.core.state

import android.content.Context
import com.horizons.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val mode: String = "standard",
)

class ChatHistoryStore(context: Context) {

    private val dir = File(context.filesDir, "chat_history").apply { mkdirs() }

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    var retentionDays: Int = 30

    init { reload() }

    fun reload() {
        val cutoff = System.currentTimeMillis() - retentionDays * 86_400_000L
        _sessions.value = dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { f -> readSession(f) }
            ?.filter { it.createdAt >= cutoff }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun save(session: ChatSession) = withContext(Dispatchers.IO) {
        val titled = if (session.title == "New chat" && session.messages.isNotEmpty()) {
            val preview = session.messages.first { it.role == "user" }.text.take(40)
            session.copy(title = preview)
        } else session
        val file = File(dir, "${titled.id}.json")
        file.writeText(titled.toJson().toString(2))
        reload()
    }

    suspend fun delete(sessionId: String) = withContext(Dispatchers.IO) {
        File(dir, "$sessionId.json").delete()
        reload()
    }

    suspend fun pruneOld() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - retentionDays * 86_400_000L
        dir.listFiles { f -> f.extension == "json" }?.forEach { f ->
            val s = readSession(f)
            if (s != null && s.createdAt < cutoff) f.delete()
        }
        reload()
    }

    fun getSession(id: String): ChatSession? =
        _sessions.value.find { it.id == id }

    private fun readSession(file: File): ChatSession? = try {
        val obj = JSONObject(file.readText())
        val msgs = obj.getJSONArray("messages").let { arr ->
            (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                ChatMessage(m.getString("role"), m.getString("text"))
            }
        }
        ChatSession(
            id = obj.getString("id"),
            title = obj.optString("title", "Untitled"),
            messages = msgs,
            createdAt = obj.optLong("createdAt", 0L),
            mode = obj.optString("mode", "standard"),
        )
    } catch (_: Exception) { null }

    private fun ChatSession.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("mode", mode)
        put("messages", JSONArray().apply {
            messages.forEach { m ->
                put(JSONObject().put("role", m.role).put("text", m.text))
            }
        })
    }
}
