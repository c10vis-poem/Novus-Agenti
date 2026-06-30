package com.horizons.ui.panels

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.state.ChatSession
import com.horizons.core.state.SavedCommand
import com.horizons.ui.SlateStoneBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)

@Composable
fun ArtifactsPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()

    val sessions by app.chatHistory.sessions.collectAsState()
    val commands by app.savedCommands.commands.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
    SlateStoneBackground()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 20.sp, color = HorizonsColors.TileArtifacts)
            }
            Text(
                "ARTIFACTS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HorizonsColors.TileArtifacts,
            )
            Text(
                "  / logs_skills",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = HorizonsColors.TileArtifacts.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Chat Archives ────────────────────────────────────────────────
        SectionHeader("Chat Archives", HorizonsColors.TileArtifacts)

        if (sessions.isEmpty()) {
            PlaceholderCard("No archived conversations yet.")
        } else {
            sessions.forEach { session ->
                ChatSessionCard(
                    session = session,
                    onDelete = { scope.launch { app.chatHistory.delete(session.id) } },
                    onShare = { shareChatSession(ctx, session) },
                )
            }
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Scripts ──────────────────────────────────────────────────────
        SectionHeader("Scripts", HorizonsColors.TileArtifacts)

        if (commands.isEmpty()) {
            PlaceholderCard("No saved scripts or commands.")
        } else {
            commands.forEach { cmd ->
                SavedCommandCard(cmd)
            }
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Boot diagnostics ─────────────────────────────────────────────
        SectionHeader("Boot Diagnostics", HorizonsColors.TileArtifacts)

        var diagText by remember { mutableStateOf(com.horizons.core.diag.Breadcrumb.readAll()) }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    diagText.takeLast(4000), // last 4 KB so we see most recent
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Refresh",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HorizonsColors.TileArtifacts,
                        modifier = Modifier.clickable {
                            diagText = com.horizons.core.diag.Breadcrumb.readAll()
                        },
                    )
                    Text(
                        "Clear",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HorizonsColors.TileSettings,
                        modifier = Modifier.clickable {
                            com.horizons.core.diag.Breadcrumb.clear()
                            diagText = com.horizons.core.diag.Breadcrumb.readAll()
                        },
                    )
                }
            }
        }

        // ── Logs ─────────────────────────────────────────────────────────
        SectionHeader("Logs", HorizonsColors.TileArtifacts)

        val logDir = remember { File(app.filesDir, "crash_logs") }
        val logFiles = remember(logDir.lastModified()) {
            if (logDir.exists() && logDir.isDirectory) {
                logDir.listFiles { f ->
                    f.extension == "txt" || f.extension == "log"
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            } else emptyList()
        }

        if (logFiles.isEmpty()) {
            PlaceholderCard("No logs recorded.")
        } else {
            logFiles.forEach { file ->
                LogFileCard(
                    file = file,
                    onShare = { shareLogFile(ctx, file) },
                )
            }
        }

        // ── Export ───────────────────────────────────────────────────────
        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        SectionHeader("Export", HorizonsColors.TileArtifacts)

        PlaceholderCard(
            "Use the share buttons on chat archives and logs above to export via the Android share sheet."
        )

        Spacer(Modifier.height(24.dp))
    }
    }
}

// ── Chat Session Card ────────────────────────────────────────────────────────

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.title,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HorizonsColors.TileArtifacts,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateFormat.format(Date(session.createdAt)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${session.messages.size} msgs",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = HorizonsColors.TileArtifacts.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                session.mode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = HorizonsColors.TileArtifacts,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = HorizonsColors.TileArtifacts.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = HorizonsColors.TileArtifacts.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    session.messages.take(5).forEach { msg ->
                        Text(
                            "${msg.role}: ${msg.text.take(120)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (session.messages.size > 5) {
                        Text(
                            "... ${session.messages.size - 5} more messages",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }
}

// ── Saved Command Card ───────────────────────────────────────────────────────

@Composable
private fun SavedCommandCard(cmd: SavedCommand) {
    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cmd.label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HorizonsColors.TileArtifacts,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = HorizonsColors.TileArtifacts.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        cmd.category,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = HorizonsColors.TileArtifacts,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                cmd.command,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Log File Card ────────────────────────────────────────────────────────────

@Composable
private fun LogFileCard(file: File, onShare: () -> Unit) {
    val preview = remember(file.lastModified()) {
        try {
            file.bufferedReader().useLines { lines ->
                lines.take(3).joinToString("\n")
            }
        } catch (_: Exception) { "" }
    }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileArtifacts,
                    )
                    Text(
                        dateFormat.format(Date(file.lastModified())),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share log",
                        tint = HorizonsColors.TileArtifacts.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    preview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Placeholder Card ─────────────────────────────────────────────────────────

@Composable
private fun PlaceholderCard(message: String) {
    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = color,
    )
}

// ── Share helpers ────────────────────────────────────────────────────────────

private fun shareChatSession(ctx: Context, session: ChatSession) {
    val text = buildString {
        appendLine("Chat: ${session.title}")
        appendLine("Date: ${dateFormat.format(Date(session.createdAt))}")
        appendLine("Mode: ${session.mode}")
        appendLine("---")
        session.messages.forEach { msg ->
            appendLine("[${msg.role}] ${msg.text}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Chat: ${session.title}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share chat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun shareLogFile(ctx: Context, file: File) {
    val text = try {
        file.readText().take(10_000)
    } catch (_: Exception) { "Could not read log file." }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Log: ${file.name}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
