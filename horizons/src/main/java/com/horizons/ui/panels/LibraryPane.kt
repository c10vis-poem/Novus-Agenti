package com.horizons.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun LibraryPane(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val vault by app.appState.snapshot.collectAsState()

    var newLabel   by remember { mutableStateOf("") }
    var newValue   by remember { mutableStateOf("") }
    var newVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Library", style = MaterialTheme.typography.titleLarge)

        // ── Key Vault ────────────────────────────────────────────────────────────
        Text("Key Vault", style = MaterialTheme.typography.titleMedium)
        Text(
            "Stored in EncryptedSharedPreferences. Add any key: API tokens, model IDs, etc.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show all vault entries except router config (those live in Router tab)
        vault.entries
            .filter { !it.key.startsWith("router.") }
            .sortedBy { it.key }
            .forEach { (key, value) ->
                VaultRow(
                    keyName = key,
                    value = value,
                    onSave = { app.appState.put(key, it) },
                    onRemove = { app.appState.remove(key) }
                )
            }

        HorizontalDivider()
        Text("Add key", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = newLabel,
            onValueChange = { newLabel = it },
            label = { Text("Key name  (e.g. openrouter.key, gemini.key)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = newValue,
            onValueChange = { newValue = it },
            label = { Text("Value") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (newVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { newVisible = !newVisible }) {
                    Text(
                        if (newVisible) "Hide" else "Show",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
        Button(
            onClick = {
                if (newLabel.isNotBlank() && newValue.isNotBlank()) {
                    app.appState.put(newLabel.trim(), newValue.trim())
                    newLabel = ""
                    newValue = ""
                }
            },
            enabled = newLabel.isNotBlank() && newValue.isNotBlank()
        ) { Text("Add to vault") }

        HorizontalDivider()

        // ── Model Library placeholder ──────────────────────────────────────────
        Text("Model Library", style = MaterialTheme.typography.titleMedium)
        Text(
            "Browse and manage models from HuggingFace and Qualcomm AI Hub.\nComing in a follow-up milestone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VaultRow(
    keyName: String,
    value: String,
    onSave: (String) -> Unit,
    onRemove: () -> Unit
) {
    var draft   by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(keyName) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None
                                   else PasswordVisualTransformation()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            TextButton(onClick = { visible = !visible }) {
                Text(
                    if (visible) "Hide" else "Show",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (draft != value) {
                TextButton(onClick = { onSave(draft.trim()) }) {
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }
            TextButton(onClick = onRemove) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
