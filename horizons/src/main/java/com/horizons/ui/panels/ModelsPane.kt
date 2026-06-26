package com.horizons.ui.panels

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.horizons.core.state.AppStateStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.horizons.HorizonsApplication

@Composable
fun ModelsPane(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication

    val appSnapshot by app.appState.snapshot.collectAsState()
    val hasStoragePermission = Environment.isExternalStorageManager()
    // Use stored override path if set, otherwise default expected location
    val modelPath = appSnapshot[AppStateStore.KEY_LITERT_MODEL_PATH]?.takeIf { it.isNotBlank() }
        ?: "/storage/emulated/0/Download/gemma-4-E2B-it.litertlm"
    val modelExists = remember(modelPath) { java.io.File(modelPath).exists() }
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val testResult by app.modelTestResult.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path: String? = runCatching {
            val docId = DocumentsContract.getDocumentId(uri)
            when {
                docId.startsWith("primary:") ->
                    "/storage/emulated/0/${docId.removePrefix("primary:")}"
                docId.startsWith("raw:") ->
                    docId.removePrefix("raw:")
                else -> {
                    // msf: IDs from the Downloads provider on Android 10+ — query _data column
                    val fromQuery = ctx.contentResolver.query(
                        uri, arrayOf("_data"), null, null, null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }
                        else null
                    }
                    // Fallback: resolve via /proc/self/fd symlink (works with MANAGE_EXTERNAL_STORAGE)
                    fromQuery ?: ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        try { android.system.Os.readlink("/proc/self/fd/${pfd.fd}") }
                        catch (_: Exception) { null }
                    }
                }
            }
        }.getOrNull()
        if (!path.isNullOrBlank()) {
            val fixed = path.replace("/mnt/user/0/emulated/", "/storage/emulated/")
            app.appState.put(AppStateStore.KEY_LITERT_MODEL_PATH, fixed)
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Models & Servers", style = MaterialTheme.typography.titleLarge)
        Text(
            "build ${com.horizons.BuildConfig.GIT_SHA} · v${com.horizons.BuildConfig.VERSION_NAME} (${com.horizons.BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.primary,
        )

        // ── Permission check ──────────────────────────────────────────────────
        if (!hasStoragePermission) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Missing: All Files Access permission",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "Horizons needs \"Allow management of all files\" to read the model from your Downloads folder. " +
                        "Without it the model won't load even if the file is there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Button(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${ctx.packageName}"))
                        )
                    }) { Text("Grant All Files Access") }
                }
            }
            HorizontalDivider()
        }

        Text("On-device LLM", style = MaterialTheme.typography.titleMedium)

        val engineStatusLabel = when {
            !hasStoragePermission -> "permission needed"
            !modelExists -> "file missing"
            backendStatus == "idle" -> "not loaded"
            backendStatus == "loading…" -> "loading…"
            backendStatus.startsWith("GPU FAILED") || backendStatus.startsWith("NPU FAILED") -> "failed"
            else -> "ready"
        }
        ServerRow(
            label = "Gemma 4 E2B  (LiteRT-LM · Adreno 830 GPU)",
            role = "Chat + Vision + STT — on-device, Backend.GPU → Adreno 830",
            status = engineStatusLabel,
        )

        if (hasStoragePermission && !modelExists) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Model file not found. Looking at:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        modelPath,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "Download gemma-4-E2B-it.litertlm (~1.5 GB) from:\n" +
                        "huggingface.co/litert-community/gemma-4-E2B-it-litert-lm\n\n" +
                        "Place in Downloads, or use Browse to locate any *.litertlm file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        } else if (modelExists) {
            Text(
                modelPath,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Backend status card — shows live loading progress / errors
        if (modelExists && backendStatus != "idle") {
            val isError = backendStatus.startsWith("GPU FAILED") || backendStatus.startsWith("NPU FAILED")
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    backendStatus,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        // Browse button — always visible so user can pick the file directly
        OutlinedButton(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Browse for model file…")
        }

        if (modelExists) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (backendStatus == "idle" || backendStatus.startsWith("GPU FAILED") || backendStatus.startsWith("NPU FAILED") || backendStatus.startsWith("NO MODEL")) {
                    Button(
                        onClick = { app.llmRuntime.preWarm() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Load Model")
                    }
                }
                if (backendStatus.startsWith("Adreno 830 · GPU") || backendStatus.startsWith("Hexagon HTP")) {
                    Button(
                        onClick = { app.testModel() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (testResult == "testing…") "Testing…" else "Test inference")
                    }
                }
            }
            if (testResult != null && testResult != "testing…") {
                val isError = testResult!!.startsWith("[LiteRtRuntime")
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        testResult!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        HorizontalDivider()

        Text("STT  (voice transcription)", style = MaterialTheme.typography.titleMedium)
        StatusChip(if (modelExists) "on-device via Gemma audio-direct" else "waiting for model")
        Text(
            "Current: PCM audio → WAV → Gemma 4 E2B audio-direct via LiteRT-LM. No network needed.\n\n" +
            "Roadmap: Silero VAD (voice activity detection, already on-device) + Whisper Base ONNX " +
            "(~74 MB, runs via onnxruntime-android). VAD triggers Whisper only when speech detected — " +
            "faster, lower power, works independently of the main LLM backend.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text("Kokoro TTS  (Sherpa-ONNX, on-device)", style = MaterialTheme.typography.titleMedium)
        StatusChip("on-device · no Android TTS broker")
        Text(
            "Horizons speaks via Sherpa-ONNX → Kokoro multi-lang v1.0. " +
            "28 English voices. Model downloads automatically on first launch (~300 MB). " +
            "No VoxSherpa or Android TextToSpeech dependency.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text("Floating Dock", style = MaterialTheme.typography.titleMedium)
        StatusChip("accessibility service")
        Text(
            "Enable Horizons in Android Settings → Accessibility → Installed services. " +
            "A 🎙 / 👁 / ⏹ dock appears on the left edge of every screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ServerRow(label: String, role: String, status: String) {
    val color = when (status) {
        "online", "ready" -> MaterialTheme.colorScheme.primary
        "offline", "file missing", "permission needed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.12f),
            ) {
                Text(
                    status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
        Text(
            role,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when {
        status.startsWith("using") || status == "ready" || status == "accessibility service" ->
            MaterialTheme.colorScheme.primary
        status.contains("fail") || status.contains("error") -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.12f)) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
