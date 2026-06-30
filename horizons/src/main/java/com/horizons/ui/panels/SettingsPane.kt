package com.horizons.ui.panels

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.horizons.HorizonsApplication
import com.horizons.core.state.AppStateStore
import com.horizons.ui.SlateStoneBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.launch

private val SettingsAccent = HorizonsColors.TileSettings

@Composable
fun SettingsPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val settings by app.settingsStore.snapshot.collectAsState()
    val creds by app.appState.snapshot.collectAsState()

    Box(modifier.fillMaxSize()) {
    SlateStoneBackground()
    Column(
        Modifier
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
                Text("←", fontSize = 20.sp, color = SettingsAccent)
            }
            Text(
                "SETTINGS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SettingsAccent,
            )
            Text(
                "  / config",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = SettingsAccent.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Credentials vault ────────────────────────────────────────────────
        SectionLabel("Credentials")

        TokenField(
            label = "HuggingFace token",
            value = creds[AppStateStore.KEY_HF_TOKEN] ?: "",
            onSave = { app.appState.put(AppStateStore.KEY_HF_TOKEN, it) },
        )
        TokenField(
            label = "GitHub token",
            value = creds[AppStateStore.KEY_GITHUB_TOKEN] ?: "",
            onSave = { app.appState.put(AppStateStore.KEY_GITHUB_TOKEN, it) },
        )

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Cloud API keys ───────────────────────────────────────────────────
        SectionLabel("Cloud APIs")
        Text(
            "Stored encrypted. Agent uses these via HttpFetch tool.",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        TokenField(
            label = "SambaNova API key",
            value = creds[AppStateStore.KEY_API_SAMBANOVA] ?: "",
            onSave = { app.appState.put(AppStateStore.KEY_API_SAMBANOVA, it) },
        )
        TokenField(
            label = "OpenRouter API key",
            value = creds[AppStateStore.KEY_API_OPENROUTER] ?: "",
            onSave = { app.appState.put(AppStateStore.KEY_API_OPENROUTER, it) },
        )
        TokenField(
            label = "QAI Hub API key",
            value = creds[AppStateStore.KEY_API_QAI_HUB] ?: "",
            onSave = { app.appState.put(AppStateStore.KEY_API_QAI_HUB, it) },
        )

        Text(
            "Cloud model ID",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        PathField(
            label = "e.g. qwen/qwen-2.5-7b-instruct",
            value = creds[com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_MODEL] ?: "",
            onSave = {
                app.appState.put(com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_MODEL, it)
                app.cloudRuntime.refreshStatus()
            },
        )

        Text(
            "Custom endpoint (blank = auto from API key)",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        PathField(
            label = "https://your-api.com/v1/chat/completions",
            value = creds[com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_ENDPOINT] ?: "",
            onSave = {
                app.appState.put(com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_ENDPOINT, it)
                app.cloudRuntime.refreshStatus()
            },
        )

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Engine settings ──────────────────────────────────────────────────
        SectionLabel("Engine")

        Text(
            "System prompt override",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        OutlinedTextField(
            value = settings.systemPromptOverride,
            onValueChange = { v -> app.settingsStore.update { it.copy(systemPromptOverride = v) } },
            label = { Text("Leave blank for engine default", color = SettingsAccent.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            maxLines = 10,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
            colors = settingsFieldColors(),
        )

        Text(
            "NPU model path",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        Text(
            "Auto-detected from Downloads. Restart after placing a new model.",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        Text(
            "Default backend ID",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        OutlinedTextField(
            value = settings.defaultBackendId,
            onValueChange = { v -> app.settingsStore.update { it.copy(defaultBackendId = v) } },
            label = { Text("Backend ID from Router (blank = auto)", color = SettingsAccent.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
            colors = settingsFieldColors(),
        )

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Debug ────────────────────────────────────────────────────────────
        SectionLabel("Debug")

        Text(
            "Log level: ${settings.debugLogLevel}  (0=off  1=verbose  2=trace)",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        Slider(
            value = settings.debugLogLevel.toFloat(),
            onValueChange = { v -> app.settingsStore.update { it.copy(debugLogLevel = v.toInt()) } },
            valueRange = 0f..2f,
            steps = 1,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsAccent.copy(alpha = 0.15f),
            ),
        )

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Verbosity ────────────────────────────────────────────────────
        SectionLabel("Verbosity")

        val verbosityLabels = listOf("Concise", "Normal", "Detailed", "Verbose")
        val currentVerbosity = creds["settings.verbosity"] ?: "Normal"
        val verbosityIndex = verbosityLabels.indexOf(currentVerbosity).coerceAtLeast(0)

        Text(
            "Response detail: $currentVerbosity",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        Slider(
            value = verbosityIndex.toFloat(),
            onValueChange = { v ->
                val label = verbosityLabels[v.toInt().coerceIn(0, 3)]
                app.appState.put("settings.verbosity", label)
            },
            valueRange = 0f..3f,
            steps = 2,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsAccent.copy(alpha = 0.15f),
            ),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            verbosityLabels.forEach { label ->
                Text(
                    label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = if (label == currentVerbosity) SettingsAccent
                            else SettingsAccent.copy(alpha = 0.35f),
                )
            }
        }

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Memory ───────────────────────────────────────────────────────
        SectionLabel("Memory")
        Text(
            "Persistent key-value memory retained by the assistant.",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        val memoryEntries = creds.filter { it.key.startsWith("memory.") }

        if (memoryEntries.isEmpty()) {
            Text(
                "(no memory entries)",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SettingsAccent.copy(alpha = 0.4f),
            )
        } else {
            memoryEntries.forEach { (key, value) ->
                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                key.removePrefix("memory."),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SettingsAccent,
                            )
                            Text(
                                value,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        TextButton(onClick = { app.appState.remove(key) }) {
                            Text(
                                "Remove",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFF6B6B),
                            )
                        }
                    }
                }
            }
        }

        // Add new memory entry
        var newMemKey by remember { mutableStateOf("") }
        var newMemVal by remember { mutableStateOf("") }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newMemKey,
                    onValueChange = { newMemKey = it },
                    label = { Text("Key", color = SettingsAccent.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                    colors = settingsFieldColors(),
                )
                OutlinedTextField(
                    value = newMemVal,
                    onValueChange = { newMemVal = it },
                    label = { Text("Value", color = SettingsAccent.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                    colors = settingsFieldColors(),
                )
                if (newMemKey.isNotBlank() && newMemVal.isNotBlank()) {
                    TextButton(onClick = {
                        app.appState.put("memory.${newMemKey.trim()}", newMemVal.trim())
                        newMemKey = ""
                        newMemVal = ""
                    }) {
                        Text("Add", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SettingsAccent)
                    }
                }
            }
        }

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Permissions ──────────────────────────────────────────────────
        SectionLabel("Permissions")

        @Composable
        fun PermissionRow(name: String, granted: Boolean) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    if (granted) "GRANTED" else "DENIED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (granted) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                )
            }
        }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                PermissionRow(
                    "RECORD_AUDIO",
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        "POST_NOTIFICATIONS",
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                PermissionRow(
                    "MANAGE_EXTERNAL_STORAGE",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    },
                )
                PermissionRow(
                    "SYSTEM_ALERT_WINDOW",
                    AndroidSettings.canDrawOverlays(ctx),
                )
            }
        }

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── System Registrations ─────────────────────────────────────────
        SectionLabel("System Registrations")
        Text(
            "These require manual setup via Android Settings.",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        @Composable
        fun RegistrationRow(name: String, status: String) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    status,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SettingsAccent.copy(alpha = 0.7f),
                )
            }
        }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                RegistrationRow("System TTS (Kokoro)", "Check System > TTS")
                RegistrationRow("Default assistant", "Check System > Assist app")
                RegistrationRow("Accessibility service", "Check System > Accessibility")
                RegistrationRow("Notification listener", "Check System > Notification access")
            }
        }

        HorizontalDivider(color = SettingsAccent.copy(alpha = 0.2f))

        // ── Chat History Retention ───────────────────────────────────────
        SectionLabel("Chat History Retention")

        val coroutineScope = rememberCoroutineScope()
        var retentionDays by remember { mutableFloatStateOf(app.chatHistory.retentionDays.toFloat()) }

        Text(
            "Keep chats for ${retentionDays.toInt()} days",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SettingsAccent.copy(alpha = 0.7f),
        )
        Slider(
            value = retentionDays,
            onValueChange = { v ->
                retentionDays = v
                app.chatHistory.retentionDays = v.toInt()
            },
            onValueChangeFinished = {
                coroutineScope.launch { app.chatHistory.pruneOld() }
            },
            valueRange = 7f..90f,
            steps = 82,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsAccent.copy(alpha = 0.15f),
            ),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("7 days", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = SettingsAccent.copy(alpha = 0.4f))
            Text("90 days", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = SettingsAccent.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = SettingsAccent,
    )
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SettingsAccent.copy(alpha = 0.6f),
    unfocusedBorderColor = SettingsAccent.copy(alpha = 0.2f),
    cursorColor = SettingsAccent,
)

@Composable
private fun TokenField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
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
                label = { Text(label, color = SettingsAccent.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                colors = settingsFieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        if (visible) "Hide" else "Show",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SettingsAccent,
                    )
                }
                if (draft != value) {
                    TextButton(onClick = { onSave(draft.trim()) }) {
                        Text("Save", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SettingsAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(label, color = SettingsAccent.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                colors = settingsFieldColors(),
            )
            if (draft != value) {
                TextButton(onClick = { onSave(draft.trim()) }) {
                    Text("Save", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SettingsAccent)
                }
            }
        }
    }
}
