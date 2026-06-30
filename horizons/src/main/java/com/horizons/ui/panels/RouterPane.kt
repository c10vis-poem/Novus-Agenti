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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.state.AppStateStore
import com.horizons.core.voice.KokoroSetupState
import com.horizons.core.voice.SherpaOnnxTtsClient
import com.horizons.ui.SlateStoneBackground
import com.horizons.ui.theme.HorizonsColors

@Composable
fun RouterPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val ttsState by app.kokoroManager.state.collectAsState()
    val testResult by app.modelTestResult.collectAsState()

    val modelPath = app.resolveNpuModelPath()
    val modelExists = modelPath != null
    val npuReady = backendStatus.startsWith("Hexagon HTP") || backendStatus.startsWith("Adreno 830")

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
                Text("←", fontSize = 20.sp, color = HorizonsColors.TileRouter)
            }
            Text(
                "ROUTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HorizonsColors.TileRouter,
            )
            Text(
                "  / route",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = HorizonsColors.TileRouter.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = HorizonsColors.TileRouter.copy(alpha = 0.2f))

        // ── NPU Runtime ──────────────────────────────────────────────────────
        Text(
            "NPU Runtime",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HorizonsColors.TileRouter,
        )

        Surface(
            color = if (npuReady) HorizonsColors.StatusAsr.copy(alpha = 0.1f)
                    else HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ort_engine · Hexagon HTP v75",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StatusPill(
                        text = if (npuReady) "ACTIVE" else if (modelExists) "IDLE" else "NO MODEL",
                        active = npuReady,
                    )
                }
                Text(
                    "Status: $backendStatus",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (modelPath != null) {
                    Text(
                        modelPath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
                if (modelExists && !npuReady) {
                    Button(onClick = { app.llmRuntime.preWarm() }) {
                        Text("Load Model", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                if (npuReady) {
                    Button(onClick = { app.testModel() }) {
                        Text(
                            if (testResult == "testing...") "Testing..." else "Test Inference",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                    if (testResult != null && testResult != "testing...") {
                        Surface(
                            color = HorizonsColors.IconBackplate,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                testResult!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = HorizonsColors.TileTerminal,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = HorizonsColors.TileRouter.copy(alpha = 0.2f))

        // ── TTS Engine ───────────────────────────────────────────────────────
        Text(
            "TTS Engine",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HorizonsColors.TileRouter,
        )

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val s = ttsState) {
                    is KokoroSetupState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Kokoro · ${SherpaOnnxTtsClient.ENGLISH_VOICES.size} voices",
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            StatusPill("NEEDS DL", active = false)
                        }
                        Button(onClick = { app.kokoroManager.ensureReady() }) {
                            Text("Download now", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                    is KokoroSetupState.Downloading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Downloading…", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text("${s.percent}%", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { s.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is KokoroSetupState.Extracting -> {
                        Text("Extracting voice model…", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is KokoroSetupState.Ready -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Kokoro · ${SherpaOnnxTtsClient.ENGLISH_VOICES.size} voices",
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            StatusPill("READY", active = true)
                        }
                    }
                    is KokoroSetupState.Error -> {
                        Text("TTS error: ${s.message}", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = HorizonsColors.TileSettings)
                        Button(onClick = { app.kokoroManager.ensureReady() }) {
                            Text("Retry", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = HorizonsColors.TileRouter.copy(alpha = 0.2f))

        // ── STT Engine ───────────────────────────────────────────────────────
        Text(
            "STT Engine",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HorizonsColors.TileRouter,
        )

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Qwen3.5-9B audio-direct", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    StatusPill(if (npuReady) "ACTIVE" else "WAITING", active = npuReady)
                }
                Text(
                    "PCM → WAV → ort_engine daemon · on-device, no network",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        HorizontalDivider(color = HorizonsColors.TileRouter.copy(alpha = 0.2f))

        // ── Cloud APIs ───────────────────────────────────────────────────────
        Text(
            "Cloud APIs",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HorizonsColors.TileRouter,
        )

        val creds by app.appState.snapshot.collectAsState()
        val cloudApis = listOf(
            "OpenRouter" to AppStateStore.KEY_API_OPENROUTER,
            "SambaNova" to AppStateStore.KEY_API_SAMBANOVA,
            "HuggingFace" to AppStateStore.KEY_HF_TOKEN,
            "QAI Hub" to AppStateStore.KEY_API_QAI_HUB,
        )

        val cloudActive = app.cloudRuntime.isConfigured
        if (cloudActive) {
            val cfg = app.cloudRuntime.resolveConfig()
            Surface(
                color = HorizonsColors.StatusAsr.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Cloud fallback active", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        StatusPill("ACTIVE", active = true)
                    }
                    if (cfg != null) {
                        Text(
                            "${cfg.label} · ${cfg.model}",
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        cloudApis.forEach { (name, storeKey) ->
            val hasKey = !creds[storeKey].isNullOrBlank()
            Surface(
                color = HorizonsColors.Surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    StatusPill(
                        text = if (hasKey) "KEY SET" else "NO KEY",
                        active = hasKey,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) HorizonsColors.StatusAsr.copy(alpha = 0.15f)
                else HorizonsColors.Surface,
    ) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) HorizonsColors.StatusAsr else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
