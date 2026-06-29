package com.horizons.ui.panels

import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.state.AppStateStore
import com.horizons.ui.SlateStoneBackground
import com.horizons.ui.theme.HorizonsColors
import java.io.File

@Composable
fun MonitorPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val vault by app.appState.snapshot.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
    SlateStoneBackground()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 20.sp, color = HorizonsColors.TileMonitor)
            }
            Text(
                "MONITOR",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HorizonsColors.TileMonitor,
            )
            Text(
                "  / cognito",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = HorizonsColors.TileMonitor.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = HorizonsColors.TileMonitor.copy(alpha = 0.2f))

        // ── Model Library ────────────────────────────────────────────────────
        Text(
            "Model Library",
            style = MaterialTheme.typography.titleMedium,
            color = HorizonsColors.TileMonitor,
            fontFamily = FontFamily.Monospace,
        )

        val modelsDir = File(app.filesDir, "models")
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
        val modelFiles = remember {
            val extensions = setOf("bin", "onnx", "gguf")
            val files = mutableListOf<File>()
            listOf(modelsDir, downloadDir).forEach { dir ->
                if (dir.isDirectory) {
                    dir.listFiles()?.filter { f ->
                        f.isFile && f.extension.lowercase() in extensions
                    }?.let { files.addAll(it) }
                }
            }
            files.sortedByDescending { it.lastModified() }
        }

        if (modelFiles.isEmpty()) {
            Surface(
                color = HorizonsColors.Surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "No model files found.\nPlace .bin, .onnx, or .gguf files in Downloads or import via browser.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            modelFiles.forEach { file ->
                val sizeMb = file.length() / (1024 * 1024)
                val active = app.resolveNpuModelPath() == file.absolutePath
                Surface(
                    color = if (active) HorizonsColors.TileMonitor.copy(alpha = 0.1f)
                            else HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                file.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) HorizonsColors.TileMonitor
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (active) "● ACTIVE" else "${sizeMb} MB",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (active) HorizonsColors.StatusAsr
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                        Text(
                            file.parent ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = HorizonsColors.TileMonitor.copy(alpha = 0.2f))

        // ── API Key / Token Vault ────────────────────────────────────────────
        Text(
            "Key Vault",
            style = MaterialTheme.typography.titleMedium,
            color = HorizonsColors.TileMonitor,
            fontFamily = FontFamily.Monospace,
        )

        Text(
            "Encrypted storage for API tokens and credentials.",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        vault.entries
            .filter { !it.key.startsWith("router.") }
            .sortedBy { it.key }
            .forEach { (key, value) ->
                VaultRow(
                    keyName = key,
                    value = value,
                    accentColor = HorizonsColors.TileMonitor,
                    onSave = { app.appState.put(key, it) },
                    onRemove = { app.appState.remove(key) },
                )
            }

        // Add new key
        var newLabel by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                label = { Text("Key name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                label = { Text("Value") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }

        Button(
            onClick = {
                if (newLabel.isNotBlank() && newValue.isNotBlank()) {
                    app.appState.put(newLabel.trim(), newValue.trim())
                    newLabel = ""
                    newValue = ""
                }
            },
            enabled = newLabel.isNotBlank() && newValue.isNotBlank(),
        ) {
            Text("Add to vault", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }

        HorizontalDivider(color = HorizonsColors.TileMonitor.copy(alpha = 0.2f))

        // ── Console ──────────────────────────────────────────────────────────
        Text(
            "Console",
            style = MaterialTheme.typography.titleMedium,
            color = HorizonsColors.TileMonitor,
            fontFamily = FontFamily.Monospace,
        )

        var consoleInput by remember { mutableStateOf("") }
        var consoleOutput by remember { mutableStateOf("ready.") }

        Surface(
            color = HorizonsColors.IconBackplate,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
        ) {
            Text(
                "> $consoleOutput",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = HorizonsColors.TileTerminal,
                modifier = Modifier.padding(12.dp),
            )
        }

        OutlinedTextField(
            value = consoleInput,
            onValueChange = { consoleInput = it },
            label = { Text("command") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val cmd = consoleInput.trim()
                consoleOutput = when {
                    cmd == "status" -> "backend: $backendStatus\nmodels: ${modelFiles.size} files"
                    cmd == "models" -> modelFiles.joinToString("\n") { "${it.name} (${it.length()/(1024*1024)} MB)" }
                    cmd.startsWith("load ") -> "load command pending — daemon integration required"
                    else -> "unknown command: $cmd"
                }
                consoleInput = ""
            }),
        )

        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun VaultRow(
    keyName: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(keyName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                visualTransformation = if (visible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        if (visible) "Hide" else "Show",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                    )
                }
                if (draft != value) {
                    TextButton(onClick = { onSave(draft.trim()) }) {
                        Text("Save", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = accentColor)
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Remove", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = HorizonsColors.TileSettings)
                }
            }
        }
    }
}
