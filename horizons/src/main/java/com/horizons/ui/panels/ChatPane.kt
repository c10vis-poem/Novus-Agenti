package com.horizons.ui.panels

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.horizons.ChatMessage
import com.horizons.ChatMode
import com.horizons.HorizonsApplication
import com.horizons.audio.AudioRecorder
import com.horizons.fgs.LiveChatService
import com.horizons.fgs.ScreenShareService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

@Composable
fun ChatPane(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val messages by app.chatMessages.collectAsState()
    val busy by app.chatBusy.collectAsState()
    val pendingJpeg by app.pendingScreenJpeg.collectAsState()
    val currentMode by app.chatMode.collectAsState()

    var input by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val recordJobRef = remember { AtomicReference<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lastText = messages.lastOrNull()?.text ?: ""
    LaunchedEffect(lastText) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val modelReady = backendStatus.startsWith("Adreno 830") || backendStatus.startsWith("Hexagon HTP")

    // MediaProjection consent launcher for Mode A
    val screenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenShareService::class.java).apply {
                putExtra(ScreenShareService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenShareService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Screen share permission denied") }
        }
    }

    fun stopFgsIfRunning() {
        context.stopService(Intent(context, LiveChatService::class.java))
        context.stopService(Intent(context, ScreenShareService::class.java))
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun selectMode(mode: ChatMode) {
        // Mic + screen-share FGS hard-require RECORD_AUDIO or startForeground() crashes.
        if ((mode == ChatMode.A || mode == ChatMode.B) && !hasMicPermission()) {
            scope.launch { snackbarHostState.showSnackbar("Grant microphone permission first (Android Settings → Apps → Horizons → Permissions)") }
            return
        }
        when (mode) {
            ChatMode.A -> {
                // Stop any running FGS first, then request consent
                stopFgsIfRunning()
                val projectionManager =
                    context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
                            as MediaProjectionManager
                screenShareLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
            ChatMode.B -> {
                stopFgsIfRunning()
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LiveChatService::class.java),
                )
            }
            ChatMode.C -> {
                stopFgsIfRunning()
                // chatMode is reset to C by the services in their onDestroy
            }
        }
    }

    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream) ?: return@use null
                    val longest = maxOf(bmp.width, bmp.height)
                    val scaled = if (longest > 1024) {
                        val s = 1024f / longest
                        Bitmap.createScaledBitmap(
                            bmp,
                            (bmp.width * s).toInt().coerceAtLeast(1),
                            (bmp.height * s).toInt().coerceAtLeast(1),
                            true
                        ).also { bmp.recycle() }
                    } else bmp
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    scaled.recycle()
                    out.toByteArray()
                }
            }
            if (bytes != null) app.pendingScreenJpeg.value = bytes
        }
    }

    fun onMicClick() {
        if (isRecording) {
            recordJobRef.getAndSet(null)?.cancel()
            isRecording = false
        } else if (!busy) {
            isRecording = true
            val job = scope.launch {
                val pcm = app.voiceLoop.recordOnce()
                isRecording = false
                if (pcm != null && pcm.isNotEmpty()) {
                    val text = app.transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
                    if (text.isNotBlank() && !text.startsWith("[")) input = text
                }
            }
            recordJobRef.set(job)
        }
    }

    fun submit() {
        val text = input.trim()
        val jpeg = app.pendingScreenJpeg.value
        if (jpeg != null) {
            app.screenAsk(jpeg, text.ifBlank { "What is in this image?" })
            app.pendingScreenJpeg.value = null
            input = ""
        } else if (text.isNotBlank() && !busy) {
            app.sendChat(text)
            input = ""
        }
    }

    Column(modifier.fillMaxSize()) {

        // ── Mode toggle row ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = currentMode == ChatMode.A,
                onClick = { selectMode(ChatMode.A) },
                label = { Text("Screen") },
            )
            FilterChip(
                selected = currentMode == ChatMode.B,
                onClick = { selectMode(ChatMode.B) },
                label = { Text("Chat") },
            )
            FilterChip(
                selected = currentMode == ChatMode.C,
                onClick = { selectMode(ChatMode.C) },
                label = { Text("One-shot") },
            )
        }

        // ── Model status banner ───────────────────────────────────────────────
        if (!modelReady) {
            val bannerMsg = when {
                backendStatus == "idle" || backendStatus == "loading…" -> "Loading model…"
                backendStatus.startsWith("NO MODEL") -> "No model found — check Router tab."
                backendStatus.startsWith("GPU FAILED") -> "Engine failed — see Router tab."
                else -> "Model not ready."
            }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    bannerMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // ── Message list ──────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { msg -> ChatBubble(msg) }
        }

        // ── Attachment thumbnail strip ─────────────────────────────────────────
        if (pendingJpeg != null) {
            val bmp: Bitmap? = remember(pendingJpeg!!.size) {
                BitmapFactory.decodeByteArray(pendingJpeg, 0, pendingJpeg!!.size)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    "Image attached — add a question and tap Ask",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { app.pendingScreenJpeg.value = null }) {
                    Text("×", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // ── Snackbar ──────────────────────────────────────────────────────────
        SnackbarHost(hostState = snackbarHostState)

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Attach button
            if (!busy) {
                IconButton(onClick = { attachLauncher.launch("image/*") }) {
                    Text("📎", style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (pendingJpeg != null) "Ask about the image…" else "Message") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
            )

            if (busy) {
                Button(
                    onClick = { app.stopAll() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                ) { Text("Stop") }
            } else {
                // Mic button
                IconButton(onClick = { onMicClick() }) {
                    Text(
                        if (isRecording) "🔴" else "🎙️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Send / Ask button
                Button(
                    onClick = { submit() },
                    enabled = (input.isNotBlank() || pendingJpeg != null),
                ) {
                    Text(if (pendingJpeg != null) "Ask" else "Send")
                }
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
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            SelectionContainer {
                Text(
                    text = msg.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
