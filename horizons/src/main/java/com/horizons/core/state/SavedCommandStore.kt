package com.horizons.core.state

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedCommand(
    val label: String,
    val command: String,
    val category: String = "general",
)

class SavedCommandStore(context: Context) {

    private val file = File(context.filesDir, "saved_commands.json")

    private val _commands = MutableStateFlow<List<SavedCommand>>(emptyList())
    val commands: StateFlow<List<SavedCommand>> = _commands.asStateFlow()

    init { reload() }

    fun reload() {
        _commands.value = if (file.exists()) {
            try {
                val arr = JSONArray(file.readText())
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    SavedCommand(
                        label = obj.getString("label"),
                        command = obj.getString("command"),
                        category = obj.optString("category", "general"),
                    )
                }
            } catch (_: Exception) { defaultCommands() }
        } else defaultCommands()
    }

    suspend fun add(cmd: SavedCommand) = withContext(Dispatchers.IO) {
        val list = _commands.value.toMutableList()
        list.removeAll { it.label == cmd.label }
        list.add(0, cmd)
        persist(list)
    }

    suspend fun remove(label: String) = withContext(Dispatchers.IO) {
        val list = _commands.value.filter { it.label != label }
        persist(list)
    }

    private fun persist(list: List<SavedCommand>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("label", c.label)
                put("command", c.command)
                put("category", c.category)
            })
        }
        file.writeText(arr.toString(2))
        _commands.value = list
    }

    private fun defaultCommands() = listOf(
        SavedCommand("System info", "uname -a", "system"),
        SavedCommand("Disk usage", "df -h", "system"),
        SavedCommand("Memory", "free -m", "system"),
        SavedCommand("IP address", "ip addr show", "network"),
        SavedCommand("List Downloads", "ls -la /storage/emulated/0/Download/", "files"),
        SavedCommand("Model files", "ls -la /storage/emulated/0/Download/*.bin 2>/dev/null; ls -la /storage/emulated/0/Download/*.onnx 2>/dev/null; ls -la /storage/emulated/0/Download/*.gguf 2>/dev/null", "models"),
        SavedCommand("Running procs", "ps -ef | head -30", "system"),
        SavedCommand("Daemon check", "ps -ef | grep ort_engine", "models"),
    )
}
