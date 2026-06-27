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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.horizons.HorizonsApplication
import com.horizons.core.voice.KokoroSetupState
import com.horizons.core.voice.SherpaOnnxTtsClient

@Composable
fun RouterPane(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication

    val ttsState by app.kokoroManager.state.collectAsState()
    val voiceId  by app.ttsVoiceId.collectAsState()
    val speed    by app.ttsSpeed.collectAsState()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Diagnostics", style = MaterialTheme.typography.titleLarge)

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "On-device inference active",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "All inference runs on-device via LiteRT-LM → Adreno 830 (Backend.GPU → OpenCL). " +
                    "No network requests or API keys required for core chat, vision, or STT.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        HorizontalDivider()

        // ── Voice / TTS ──────────────────────────────────────────────────────────
        Text("Voice (TTS)", style = MaterialTheme.typography.titleMedium)

        when (val s = ttsState) {
            is KokoroSetupState.Idle -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Kokoro on-device TTS — ${SherpaOnnxTtsClient.ENGLISH_VOICES.size} English voices",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Model not yet downloaded (~200 MB). Download starts automatically at next app launch, or tap below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { app.kokoroManager.ensureReady() }) {
                            Text("Download now")
                        }
                    }
                }
            }

            is KokoroSetupState.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Downloading Kokoro voices…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "${s.percent}% of ${"%.0f".format(s.totalMb)} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { s.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is KokoroSetupState.Extracting -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Extracting voice model…", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            is KokoroSetupState.Ready -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Kokoro · ${SherpaOnnxTtsClient.ENGLISH_VOICES.size} voices · ready",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Active: ${SherpaOnnxTtsClient.ENGLISH_VOICES.firstOrNull { it.id == voiceId }?.label ?: voiceId}  ·  speed ${speed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // TODO (design agent): voice picker, speed slider, pitch control
                    }
                }
            }

            is KokoroSetupState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TTS setup error", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(s.message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { app.kokoroManager.ensureReady() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Upcoming cloud connectors", style = MaterialTheme.typography.titleMedium)
        Text(
            "• HuggingFace Inference API\n" +
            "• Qualcomm AI Hub\n" +
            "• OpenRouter\n" +
            "• OpenAI / Anthropic\n" +
            "• Gemini / Vertex AI\n" +
            "• Custom OpenAI-compatible endpoint",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
