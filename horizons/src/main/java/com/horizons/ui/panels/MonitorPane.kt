package com.horizons.ui.panels

import android.os.Environment
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.ModelImportActivity
import com.horizons.core.state.ConfigStatus
import com.horizons.ui.OscilloscopeBackground
import com.horizons.ui.theme.HorizonsColors
import java.io.File

private val Accent = HorizonsColors.TileMonitor
private val ReadyGreen = Color(0xFF4CAF50)
private val WarningAmber = Color(0xFFE8A838)
private val ErrorRed = Color(0xFFFF5577)

@Composable
fun MonitorPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val configs by app.routerConfigs.configs.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val modelsDir = File(app.filesDir, "models")
    val downloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    )

    val modelFiles = remember(configs.hashCode()) {
        val files = mutableListOf<File>()
        listOf(modelsDir, downloadDir).forEach { dir ->
            if (dir.isDirectory) {
                dir.listFiles()?.filter { f ->
                    f.isFile && ModelImportActivity.MODEL_EXTENSIONS.any {
                        f.name.lowercase().endsWith(it)
                    }
                }?.let { files.addAll(it) }
            }
        }
        files.sortedByDescending { it.lastModified() }
    }

    val runtimeFiles = remember(configs.hashCode()) {
        val files = mutableListOf<File>()
        listOf(app.filesDir, downloadDir).forEach { dir ->
            if (dir.isDirectory) {
                dir.listFiles()?.filter { f ->
                    f.isFile && ModelImportActivity.RUNTIME_FILES.any {
                        f.name.equals(it, ignoreCase = true)
                    }
                }?.let { files.addAll(it) }
            }
        }
        files.sortedBy { it.name }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OscilloscopeBackground()
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp, color = Accent)
                    }
                    Text(
                        "MONITOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Accent,
                    )
                    Text(
                        "  / console",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Accent.copy(alpha = 0.5f),
                    )
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── System Status Dashboard ─────────────────────────────────
                Text(
                    "System Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                val isNoBackend = backendStatus.contains("no backend")
                val isCloud = backendStatus.contains("Cloud")
                val isNpu = backendStatus.contains("Hexagon") || backendStatus.contains("NPU")
                val activeModel = app.resolveNpuModelPath()

                val readyCount = configs.count { it.isReady || it.status == ConfigStatus.RUNNING }
                val runningCount = configs.count { it.status == ConfigStatus.RUNNING }
                val incompleteCount = configs.count {
                    !it.isReady && it.status != ConfigStatus.ARCHIVED && it.status != ConfigStatus.SLEEPING
                }

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusRow(
                            label = "Backend",
                            value = backendStatus,
                            color = when {
                                isNoBackend -> ErrorRed
                                isCloud -> WarningAmber
                                isNpu -> ReadyGreen
                                else -> Accent
                            },
                        )
                        StatusRow(
                            label = "Active model",
                            value = activeModel?.substringAfterLast("/") ?: "none",
                            color = if (activeModel != null) ReadyGreen else ErrorRed,
                        )
                        StatusRow(
                            label = "Router configs",
                            value = "$readyCount ready / $runningCount running / $incompleteCount incomplete",
                            color = if (readyCount > 0) ReadyGreen else WarningAmber,
                        )
                        StatusRow(
                            label = "Models on disk",
                            value = "${modelFiles.size} files",
                            color = if (modelFiles.isNotEmpty()) ReadyGreen else WarningAmber,
                        )
                        StatusRow(
                            label = "Runtimes on disk",
                            value = if (runtimeFiles.isEmpty()) "none" else runtimeFiles.joinToString { it.name },
                            color = if (runtimeFiles.isNotEmpty()) ReadyGreen else WarningAmber,
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Model Library ───────────────────────────────────────────
                Text(
                    "Model Library",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "Browse models and runtimes on disk. Tap to copy path.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                if (modelFiles.isEmpty() && runtimeFiles.isEmpty()) {
                    Surface(
                        color = HorizonsColors.Surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "No model or runtime files found.\n" +
                                "Import via Settings (vault) or use \"Open with → Horizons\" on any\n" +
                                ".gguf / .onnx / .bin / .dlc / .pte / .tflite / .qnn file.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                if (runtimeFiles.isNotEmpty()) {
                    Text(
                        "RUNTIMES",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Accent.copy(alpha = 0.6f),
                    )
                    runtimeFiles.forEach { file ->
                        val sizeMb = file.length() / (1024 * 1024)
                        val isExec = file.canExecute()
                        LibraryFileCard(
                            name = file.name,
                            path = file.parent ?: "",
                            sizeInfo = "${sizeMb} MB",
                            compatInfo = when {
                                file.name.startsWith("ort_engine") ->
                                    "Runtime: ort_engine (ORT+QNN)" + if (isExec) " ✓ executable" else " ✗ not executable"
                                file.name.startsWith("libonnxruntime") -> "Lib: ONNX Runtime"
                                file.name.startsWith("libQnn") -> "Lib: QNN (Hexagon HTP)"
                                else -> "Runtime component"
                            },
                            compatColor = ReadyGreen,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(file.absolutePath))
                            },
                        )
                    }
                }

                if (modelFiles.isNotEmpty()) {
                    Text(
                        "MODELS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Accent.copy(alpha = 0.6f),
                    )
                    modelFiles.forEach { file ->
                        val sizeMb = file.length() / (1024 * 1024)
                        val ext = file.extension.lowercase()
                        val active = app.resolveNpuModelPath() == file.absolutePath

                        val (compat, compatClr) = when (ext) {
                            "gguf" -> "Compatible: GenieX (GGML backend)" to ReadyGreen
                            "onnx" -> "Compatible: ort_engine (ORT+QNN EP)" to ReadyGreen
                            "bin" -> "Compatible: ort_engine / GenieX" to ReadyGreen
                            "dlc" -> "Compatible: SNPE / QNN direct" to Accent
                            "pte" -> "Compatible: ExecuTorch" to Accent
                            "tflite" -> "Compatible: TFLite" to Accent
                            "qnn" -> "Compatible: QNN context binary" to ReadyGreen
                            else -> "Unknown format" to WarningAmber
                        }

                        LibraryFileCard(
                            name = file.name,
                            path = file.parent ?: "",
                            sizeInfo = if (active) "● ACTIVE" else "${sizeMb} MB",
                            compatInfo = compat +
                                if (active) " — currently loaded" else "",
                            compatColor = if (active) ReadyGreen else compatClr,
                            highlighted = active,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(file.absolutePath))
                            },
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Compatibility Guide ─────────────────────────────────────
                Text(
                    "Compatibility Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompatRow("GenieX", "GGML backend", ".gguf")
                        CompatRow("GenieX", "QAIRT backend", ".dlc / .qnn")
                        CompatRow("ort_engine", "ORT + QNN EP", ".onnx / .bin")
                        CompatRow("Cloud API", "OpenRouter / custom", "API key + endpoint")
                        CompatRow("Terminal", "Custom script", "Bash / harness")
                        CompatRow("CLI", "On-device", "No model needed")
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Console ─────────────────────────────────────────────────
                Text(
                    "Console",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                var consoleInput by remember { mutableStateOf("") }
                var consoleOutput by remember { mutableStateOf("ready. type 'help' for commands.") }

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
                            cmd == "help" -> "commands: status, models, runtimes, configs, compat"
                            cmd == "status" -> buildString {
                                append("backend: $backendStatus\n")
                                append("active model: ${activeModel?.substringAfterLast("/") ?: "none"}\n")
                                append("models: ${modelFiles.size} files\n")
                                append("runtimes: ${runtimeFiles.size} files\n")
                                append("router configs: ${configs.size} total, $readyCount ready, $runningCount running")
                            }
                            cmd == "models" -> if (modelFiles.isEmpty()) "no model files found"
                                else modelFiles.joinToString("\n") { "${it.name} (${it.length() / (1024 * 1024)} MB)" }
                            cmd == "runtimes" -> if (runtimeFiles.isEmpty()) "no runtime files found"
                                else runtimeFiles.joinToString("\n") { "${it.name} (${it.length() / (1024 * 1024)} MB)" }
                            cmd == "configs" -> if (configs.isEmpty()) "no router configs"
                                else configs.joinToString("\n") { "${it.name} [${it.status.name}] - ${it.runtime.ifBlank { "no runtime" }}" }
                            cmd == "compat" -> "gguf->GenieX | onnx/bin->ort_engine | dlc/qnn->QNN | cloud->API key+endpoint"
                            cmd.startsWith("load ") -> "use Router to load configs — this is a read-only console"
                            else -> "unknown: $cmd — type 'help'"
                        }
                        consoleInput = ""
                    }),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryFileCard(
    name: String,
    path: String,
    sizeInfo: String,
    compatInfo: String,
    compatColor: Color,
    highlighted: Boolean = false,
    onCopy: () -> Unit,
) {
    var copied by remember { mutableStateOf(false) }
    Surface(
        color = if (highlighted) Accent.copy(alpha = 0.1f) else HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCopy()
                copied = true
            },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) Accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    if (copied) "COPIED" else sizeInfo,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (copied) ReadyGreen
                        else if (highlighted) ReadyGreen
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Text(
                compatInfo,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = compatColor.copy(alpha = 0.8f),
            )
            Text(
                path,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun CompatRow(runtime: String, backend: String, formats: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            runtime,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Accent,
            modifier = Modifier.width(90.dp),
        )
        Text(
            backend,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(110.dp),
        )
        Text(
            formats,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
