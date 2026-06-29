package com.horizons.ui.panels

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.horizons.HorizonsApplication
import com.horizons.core.state.AppStateStore

@Composable
fun SettingsPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val settings by app.settingsStore.snapshot.collectAsState()
    val creds by app.appState.snapshot.collectAsState()

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // ── Credentials vault ────────────────────────────────────────────────
        Text("Credentials", style = MaterialTheme.typography.titleMedium)

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

        HorizontalDivider()

        // ── Cloud API keys (neuromesh — used by agent HttpFetch tool) ────────────
        Text("Cloud APIs", style = MaterialTheme.typography.titleMedium)
        Text(
            "Stored encrypted. Agent uses these when calling SambaNova, OpenRouter, etc.",
            style = MaterialTheme.typography.bodySmall,
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

        HorizontalDivider()

        // ── Engine settings ──────────────────────────────────────────────────
        Text("Engine", style = MaterialTheme.typography.titleMedium)

        Text("System prompt override", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = settings.systemPromptOverride,
            onValueChange = { v -> app.settingsStore.update { it.copy(systemPromptOverride = v) } },
            label = { Text("Leave blank to use engine default") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            maxLines = 10,
        )
        Text(
            "System prompt override (not currently applied by the ort_engine daemon).",
            style = MaterialTheme.typography.bodySmall
        )

        Text("NPU model path", style = MaterialTheme.typography.titleSmall)
        Text(
            "Model .bin file is auto-detected from Downloads. " +
            "Restart app after placing a new model.",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Default backend ID", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = settings.defaultBackendId,
            onValueChange = { v -> app.settingsStore.update { it.copy(defaultBackendId = v) } },
            label = { Text("Backend ID from Router (blank = auto)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(
            "Debug log level: ${settings.debugLogLevel}  (0=off  1=verbose  2=trace)",
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = settings.debugLogLevel.toFloat(),
            onValueChange = { v -> app.settingsStore.update { it.copy(debugLogLevel = v.toInt()) } },
            valueRange = 0f..2f,
            steps = 1,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PathField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            if (draft != value) {
                androidx.compose.material3.TextButton(onClick = { onSave(draft.trim()) }) {
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    )
}

@Composable
private fun TokenField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.TextButton(onClick = { visible = !visible }) {
                    Text(if (visible) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
                if (draft != value) {
                    androidx.compose.material3.TextButton(onClick = { onSave(draft.trim()) }) {
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
    )
}
