package com.horizons.uilocal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.horizons.ChatMessage
import com.horizons.HorizonsApplication
import com.horizons.core.shell.DaemonLauncher
import com.horizons.core.stt.DaemonSttClient
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private enum class DaemonReadiness { CHECKING, READY, LOADING, OFFLINE }

/**
 * The local UI fork's home screen (session 16). Two independent, non-blocking
 * status rows — model+vision daemon (127.0.0.1:$ENGINE_PORT) and the media
 * (STT/TTS) daemon (127.0.0.1:8091) — plus a chat surface wired straight to
 * [HorizonsApplication.llmRuntime] via sendChat(). Neither daemon's state gates
 * the chat input: sending before either daemon is ready just surfaces the same
 * honest "[not ready]" messages NpuClient/the fallback runtime already emit.
 */
@Composable
fun LocalHomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication

    var modelDaemon by remember { mutableStateOf(DaemonReadiness.CHECKING) }
    var mediaDaemon by remember { mutableStateOf(DaemonReadiness.CHECKING) }

    LaunchedEffect(Unit) {
        while (true) {
            modelDaemon = probeDaemon("http://127.0.0.1:${DaemonLauncher.ENGINE_PORT}/health")
            mediaDaemon = probeDaemon("${DaemonSttClient.DEFAULT_BASE}/health")
            delay(3_000)
        }
    }

    val messages by app.chatMessages.collectAsState()
    val busy by app.chatBusy.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "Novus Agenti — Local",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "UI fork · boots without waiting on the model",
            style = MaterialTheme.typography.bodySmall,
            color = HorizonsColors.HighlightTeal,
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DaemonStatusChip(
                label = "Model + Vision",
                sublabel = "127.0.0.1:${DaemonLauncher.ENGINE_PORT}",
                state = modelDaemon,
                modifier = Modifier.weight(1f),
            )
            DaemonStatusChip(
                label = "Media (STT/TTS)",
                sublabel = DaemonSttClient.DEFAULT_BASE.removePrefix("http://"),
                state = mediaDaemon,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages) { msg -> ChatBubble(msg) }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Novus Agenti…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && !busy) {
                        app.sendChat(input)
                        input = ""
                    }
                }),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = !busy,
                onClick = {
                    if (input.isNotBlank()) {
                        app.sendChat(input)
                        input = ""
                    }
                },
            ) { Text(if (busy) "…" else "Send") }
        }
    }
}

@Composable
private fun DaemonStatusChip(
    label: String,
    sublabel: String,
    state: DaemonReadiness,
    modifier: Modifier = Modifier,
) {
    val (dotColor, statusText) = when (state) {
        DaemonReadiness.READY -> HorizonsColors.StatusAsr to "ready"
        DaemonReadiness.LOADING -> HorizonsColors.ActionYellow to "loading…"
        DaemonReadiness.OFFLINE -> HorizonsColors.TileSettings to "offline (optional)"
        DaemonReadiness.CHECKING -> HorizonsColors.PrimaryTeal to "checking…"
    }
    Surface(
        modifier = modifier,
        color = HorizonsColors.Surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("$sublabel · $statusText", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) HorizonsColors.PrimaryTeal.copy(alpha = 0.25f) else HorizonsColors.Surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                msg.text.ifBlank { "…" },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** GET {url}/health → READY (200) · LOADING (503) · OFFLINE (unreachable/other). */
private suspend fun probeDaemon(url: String): DaemonReadiness = withContext(Dispatchers.IO) {
    try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 1_000
        conn.readTimeout = 1_000
        val code = conn.responseCode
        conn.disconnect()
        when (code) {
            200 -> DaemonReadiness.READY
            503 -> DaemonReadiness.LOADING
            else -> DaemonReadiness.OFFLINE
        }
    } catch (_: Exception) {
        DaemonReadiness.OFFLINE
    }
}
