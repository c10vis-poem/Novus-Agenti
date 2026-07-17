package com.horizons.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.llm.CloudLlmRuntime
import com.horizons.core.state.AppStateStore
import com.horizons.core.voice.KokoroSetupState
import com.horizons.core.voice.SherpaOnnxTtsClient
import com.horizons.ui.SlateStoneBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val Accent = HorizonsColors.TileRouter

@Composable
fun RouterPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val ttsState by app.kokoroManager.state.collectAsState()
    val testResult by app.modelTestResult.collectAsState()
    val creds by app.appState.snapshot.collectAsState()
    val currentVoiceId by app.ttsVoiceId.collectAsState()
    val currentSpeed by app.ttsSpeed.collectAsState()

    // GenieX model selection is explicit, operator-driven — NOT auto-picked
    // from whatever's in Downloads (session 17 hard rule: ship/run empty
    // until the operator loads something). This list is just what's
    // available to choose from.
    val genieXCandidates = remember { app.listGenieXModelCandidates() }
    val activeGenieXModel = app.activeGenieXModelPath
    val npuReady = backendStatus.startsWith("Hexagon HTP") || backendStatus.startsWith("Adreno 830")
    val perf by app.llmRuntime.perfMetrics.collectAsState()
    val isCloudBackend = backendStatus.contains("Cloud")

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
                Text("←", fontSize = 20.sp, color = Accent)
            }
            Text(
                "ROUTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Accent,
            )
            Text(
                "  / route",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Accent.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── NPU Runtime — GenieX, explicit load/swap/unload ────────────────────
        // Nothing here auto-runs. The app ships/lands/runs empty; the operator
        // picks a model, which is the ONLY thing that makes CliffordService
        // launch geniex_daemon (AppStateStore.KEY_ACTIVE_GENIEX_MODEL).
        // "Runtime", not "NPU Runtime" — GenieX schedules across NPU/GPU/CPU
        // (its llama_cpp backend defaults to hybrid dispatch); the hardware
        // split is the runtime's business, not a category the UI should
        // hardcode (operator-corrected, session 17).
        RouterSection("Runtime — GenieX")

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
                        "geniex_daemon · Hexagon HTP v79",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StatusPill(
                        text = when {
                            npuReady -> "ACTIVE"
                            activeGenieXModel != null -> "LOADING"
                            else -> "EMPTY"
                        },
                        active = npuReady,
                    )
                }
                Text(
                    "Status: $backendStatus",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                if (genieXCandidates.isEmpty()) {
                    Text(
                        "No .gguf files found in models/ or Download/.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                } else {
                    Text(
                        "Pick a model to load — nothing runs until you choose one:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    genieXCandidates.forEach { file ->
                        val isSelected = file.absolutePath == activeGenieXModel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Accent else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${file.length() / (1024 * 1024)} MB",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                            if (isSelected) {
                                OutlinedButton(onClick = { app.setActiveGenieXModel(null) }) {
                                    Text("Unload", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            } else {
                                Button(onClick = { app.setActiveGenieXModel(file.absolutePath) }) {
                                    Text("Load", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
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

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── Performance Metrics ──────────────────────────────────────────────
        RouterSection("Performance")

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (perf == null) {
                Text(
                    "No inference run yet this session — send a chat message to populate.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val p = perf!!
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "First token",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            "${p.firstTokenMs} ms",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (isCloudBackend) "Stream rate (approx)" else "Tokens/sec",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            "%.1f tok/s".format(p.tokensPerSec),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Tokens this reply",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            "${p.tokenCount}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    val memInfo = remember {
                        android.app.ActivityManager.MemoryInfo().also {
                            (ctx.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                                .getMemoryInfo(it)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Device memory used",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            "${(memInfo.totalMem - memInfo.availMem) / (1024 * 1024)} MB / ${memInfo.totalMem / (1024 * 1024)} MB",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── Cloud Model Selector ─────────────────────────────────────────────
        RouterSection("Cloud Model Selector")

        val cloudCfg = app.cloudRuntime.resolveConfig()
        val activeCloudModel = creds[CloudLlmRuntime.KEY_CLOUD_MODEL]?.takeIf { it.isNotBlank() }
            ?: cloudCfg?.model ?: "(none)"
        val activeProvider = cloudCfg?.label ?: "not configured"

        Surface(
            color = if (cloudCfg != null) HorizonsColors.StatusAsr.copy(alpha = 0.1f)
                    else HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Provider: $activeProvider",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    StatusPill(
                        text = if (cloudCfg != null) "LINKED" else "OFFLINE",
                        active = cloudCfg != null,
                    )
                }
                Text(
                    "Model: $activeCloudModel",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
                val customEndpoint = creds[CloudLlmRuntime.KEY_CLOUD_ENDPOINT]?.takeIf { it.isNotBlank() }
                if (customEndpoint != null) {
                    Text(
                        "Endpoint: $customEndpoint",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // Manual model ID input
        var modelDraft by remember { mutableStateOf("") }
        OutlinedTextField(
            value = modelDraft,
            onValueChange = { modelDraft = it },
            label = { Text("Set model ID (e.g. qwen/qwen-2.5-7b-instruct)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        Button(
            onClick = {
                if (modelDraft.isNotBlank()) {
                    app.appState.put(CloudLlmRuntime.KEY_CLOUD_MODEL, modelDraft.trim())
                    app.cloudRuntime.refreshStatus()
                    modelDraft = ""
                }
            },
            enabled = modelDraft.isNotBlank(),
        ) {
            Text("Set Model", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }

        // Custom endpoint input
        var endpointDraft by remember { mutableStateOf("") }
        OutlinedTextField(
            value = endpointDraft,
            onValueChange = { endpointDraft = it },
            label = { Text("Custom endpoint (blank = auto from API key)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    app.appState.put(CloudLlmRuntime.KEY_CLOUD_ENDPOINT, endpointDraft.trim())
                    app.cloudRuntime.refreshStatus()
                    endpointDraft = ""
                },
            ) {
                Text("Set Endpoint", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            if (creds[CloudLlmRuntime.KEY_CLOUD_ENDPOINT]?.isNotBlank() == true) {
                OutlinedButton(onClick = {
                    app.appState.put(CloudLlmRuntime.KEY_CLOUD_ENDPOINT, "")
                    app.cloudRuntime.refreshStatus()
                }) {
                    Text("Clear", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        // Test connection
        if (cloudCfg != null) {
            var testing by remember { mutableStateOf(false) }
            var testMsg by remember { mutableStateOf<String?>(null) }
            OutlinedButton(
                onClick = {
                    testing = true
                    testMsg = null
                    scope.launch {
                        testMsg = withContext(Dispatchers.IO) {
                            testCloudConnection(cloudCfg)
                        }
                        testing = false
                    }
                },
                enabled = !testing,
            ) {
                if (testing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Accent)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Test Connection", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            if (testMsg != null) {
                Text(
                    testMsg!!,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (testMsg!!.startsWith("OK")) HorizonsColors.StatusAsr
                            else HorizonsColors.TileSettings,
                )
            }
        }

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── OpenRouter Model Catalog ─────────────────────────────────────────
        val hasOpenRouterKey = !creds[AppStateStore.KEY_API_OPENROUTER].isNullOrBlank()

        if (hasOpenRouterKey) {
            RouterSection("OpenRouter Catalog")

            var orModels by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            var orLoading by remember { mutableStateOf(false) }
            var orError by remember { mutableStateOf<String?>(null) }

            OutlinedButton(
                onClick = {
                    orLoading = true
                    orError = null
                    scope.launch {
                        try {
                            orModels = withContext(Dispatchers.IO) { fetchOpenRouterModels() }
                        } catch (e: Exception) {
                            orError = e.message ?: "fetch failed"
                        }
                        orLoading = false
                    }
                },
                enabled = !orLoading,
            ) {
                if (orLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Accent)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (orModels.isEmpty()) "Load OpenRouter Models" else "Refresh (${orModels.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }

            if (orError != null) {
                Text(orError!!, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = HorizonsColors.TileSettings)
            }

            orModels.take(60).forEach { (id, name) ->
                val isActive = activeCloudModel == id
                Surface(
                    color = if (isActive) Accent.copy(alpha = 0.1f) else HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.small,
                    border = if (isActive) BorderStroke(1.dp, Accent.copy(alpha = 0.4f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            app.appState.put(CloudLlmRuntime.KEY_CLOUD_MODEL, id)
                            app.cloudRuntime.refreshStatus()
                        },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Accent else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                id,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                        if (isActive) StatusPill("ACTIVE", active = true)
                    }
                }
            }

            HorizontalDivider(color = Accent.copy(alpha = 0.2f))
        }

        // ── Cloud API Keys Status ────────────────────────────────────────────
        RouterSection("Cloud APIs")

        val cloudApis = listOf(
            "OpenRouter" to AppStateStore.KEY_API_OPENROUTER,
            "SambaNova" to AppStateStore.KEY_API_SAMBANOVA,
            "HuggingFace" to AppStateStore.KEY_HF_TOKEN,
            "QAI Hub" to AppStateStore.KEY_API_QAI_HUB,
        )

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

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── TTS Engine + Voice Picker ────────────────────────────────────────
        RouterSection("TTS Engine")

        // LEGACY NOTICE: the in-process Kokoro engine below is not wired to
        // playback right now — its one auto-init call site was removed
        // (session 17) because a native init crash was killing the app on
        // launch. Downloading/previewing here is currently a dead end; real
        // TTS output is moving to the media daemon (:8091). Not silently
        // hidden so this doesn't look like ANOTHER unexplained non-response.
        Surface(
            color = HorizonsColors.TileSettings.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "LEGACY — in-process engine, not wired to playback right now. " +
                    "Preview/Download below won't produce audio. Real TTS is " +
                    "moving to the media daemon.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = HorizonsColors.TileSettings,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(Modifier.height(6.dp))

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

        // Voice picker
        Text(
            "Voice: $currentVoiceId · Speed: ${"%.1f".format(currentSpeed)}x",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Accent.copy(alpha = 0.7f),
        )

        Slider(
            value = currentSpeed,
            onValueChange = { app.ttsSpeed.value = it },
            valueRange = 0.5f..2.0f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Accent.copy(alpha = 0.15f),
            ),
        )

        SherpaOnnxTtsClient.ENGLISH_VOICES.forEach { voice ->
            val isActive = voice.id == currentVoiceId
            Surface(
                color = if (isActive) Accent.copy(alpha = 0.1f) else HorizonsColors.Surface,
                shape = MaterialTheme.shapes.small,
                border = if (isActive) BorderStroke(1.dp, Accent.copy(alpha = 0.4f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { app.ttsVoiceId.value = voice.id },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            voice.label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Accent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            voice.id,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                    if (isActive) {
                        StatusPill("ACTIVE", active = true)
                        Spacer(Modifier.width(4.dp))
                    }
                    TextButton(onClick = {
                        app.ttsVoiceId.value = voice.id
                        scope.launch { app.tts.speak("Hello, I am ${voice.label.substringAfter("· ")}.") }
                    }) {
                        Text("Preview", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Accent)
                    }
                }
            }
        }

        HorizontalDivider(color = Accent.copy(alpha = 0.2f))

        // ── STT Engine ───────────────────────────────────────────────────────
        RouterSection("STT Engine")

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

        Spacer(Modifier.height(24.dp))
    }
    }
}

private fun testCloudConnection(cfg: CloudLlmRuntime.CloudConfig): String {
    return try {
        val conn = URL(cfg.endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${cfg.apiKey}")
        conn.connectTimeout = 8_000
        conn.readTimeout = 15_000
        val body = """{"model":"${cfg.model}","messages":[{"role":"user","content":"ping"}],"max_tokens":1,"stream":false}"""
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        conn.disconnect()
        if (code in 200..299) "OK · ${cfg.label} responded HTTP $code"
        else "HTTP $code — check API key and model ID"
    } catch (e: Exception) {
        "Connection failed: ${e.message}"
    }
}

private fun fetchOpenRouterModels(): List<Pair<String, String>> {
    val conn = URL("https://openrouter.ai/api/v1/models").openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        if (conn.responseCode !in 200..299) return emptyList()
        val body = conn.inputStream.bufferedReader().readText()
        val arr = JSONObject(body).getJSONArray("data")
        val result = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id", "")
            val name = obj.optString("name", id)
            if (id.isNotBlank()) result.add(id to name)
        }
        return result.sortedBy { it.second.lowercase() }
    } finally {
        conn.disconnect()
    }
}

@Composable
private fun RouterSection(title: String) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Accent,
    )
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
