package com.horizons.ui.panels

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MatrixGreen = Color(0xFF00FF41)
private val MatrixDimGreen = Color(0xFF003B0F)

private data class RainColumn(
    val col: Int,
    val speed: Float,
    val chars: List<Char>,
    val startOffset: Float,
)

private data class ShellEntry(val cmd: String, val stdout: String, val stderr: String, val exitCode: Int)
private data class TermEntry(val input: String, val result: String, val ok: Boolean)

@Composable
fun TerminalPanel(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    val rainColumns = remember { generateRainColumns(40) }
    val transition = rememberInfiniteTransition(label = "matrix")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain",
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Matrix waterfall background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMatrixRain(rainColumns, animProgress)
        }

        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, color = MatrixGreen)
                }
                Text(
                    "TERMINAL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MatrixGreen,
                )
                Text(
                    "  / shell",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MatrixGreen.copy(alpha = 0.5f),
                )
            }

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = MatrixGreen,
                indicator = {},
                divider = { HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f)) },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Shell",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 0) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Tasker",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 1) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
            }

            when (selectedTab) {
                0 -> ShellTab(app = app, scope = scope)
                1 -> TaskerTab(app = app, scope = scope)
            }
        }
    }
}

private fun generateRainColumns(count: Int): List<RainColumn> {
    val rng = java.util.Random(7)
    val caretChar = '^'
    return List(count) { i ->
        RainColumn(
            col = i,
            speed = 0.3f + rng.nextFloat() * 0.7f,
            chars = List(8 + rng.nextInt(12)) { caretChar },
            startOffset = rng.nextFloat(),
        )
    }
}

private fun DrawScope.drawMatrixRain(columns: List<RainColumn>, progress: Float) {
    val colWidth = size.width / columns.size
    val charHeight = 18f

    columns.forEach { col ->
        val x = col.col * colWidth + colWidth / 2f
        val totalHeight = col.chars.size * charHeight
        val cyclePos = ((progress * col.speed + col.startOffset) % 1f) * (size.height + totalHeight)

        col.chars.forEachIndexed { i, _ ->
            val y = cyclePos - i * charHeight
            if (y in -charHeight..size.height + charHeight) {
                val fade = when {
                    i == 0 -> 0.9f
                    i < 3 -> 0.5f - i * 0.1f
                    else -> 0.15f - (i - 3) * 0.015f
                }.coerceIn(0.02f, 0.9f)

                drawCircle(
                    color = Color(0xFF00FF41).copy(alpha = fade * 0.3f),
                    radius = 4f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

@Composable
private fun ShellTab(
    app: HorizonsApplication,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val listState = rememberLazyListState()
    var cmd by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<ShellEntry>() }

    fun runCmd() {
        val command = cmd.trim().ifEmpty { return }
        running = true
        scope.launch {
            val result = app.tasker.runShellCommand(command)
            history.add(ShellEntry(command, result.stdout, result.stderr, result.exitCode))
            if (result.exitCode == 0) cmd = ""
            running = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!app.tasker.isTermuxInstalled()) {
            Surface(
                color = Color(0xFF1A0000),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Termux not installed — install from F-Droid and grant RUN_COMMAND permission.",
                    color = Color(0xFFFF6666),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history.asReversed()) { entry ->
                val isError = entry.exitCode != 0
                val color = if (isError) Color(0xFFFF4444) else MatrixGreen
                val output = when {
                    entry.stdout.isNotEmpty() && entry.stderr.isNotEmpty() ->
                        "${entry.stdout}\n[stderr] ${entry.stderr}"
                    entry.stdout.isNotEmpty() -> entry.stdout
                    entry.stderr.isNotEmpty() -> entry.stderr
                    else -> "(no output, exit ${entry.exitCode})"
                }
                Text(
                    "$ ${entry.cmd}\n$output",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = color,
                )
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        OutlinedTextField(
            value = cmd,
            onValueChange = { cmd = it },
            label = { Text("Shell command", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !running,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { runCmd() }),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = ::runCmd,
                enabled = !running && cmd.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreen.copy(alpha = 0.15f),
                    contentColor = MatrixGreen,
                ),
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MatrixGreen,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running…", fontFamily = FontFamily.Monospace)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run", fontFamily = FontFamily.Monospace)
                }
            }
            OutlinedButton(
                onClick = { history.clear() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear")
            }
        }
    }
}

@Composable
private fun TaskerTab(
    app: HorizonsApplication,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val listState = rememberLazyListState()

    var taskName by remember { mutableStateOf("") }
    var param1 by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<TermEntry>() }

    fun dispatch() {
        val name = taskName.trim().ifEmpty { return }
        val p = param1.trim()
        val params = if (p.isNotEmpty()) arrayOf("p1" to p) else emptyArray()
        val res = app.tasker.runTask(name, *params)
        val ok = res.isSuccess
        val msg = if (ok) "fired: $name${if (p.isNotEmpty()) " [$p]" else ""}"
                  else "error: ${res.exceptionOrNull()?.message ?: "unknown"}"
        history.add(TermEntry(name, msg, ok))
        if (ok) { taskName = ""; param1 = "" }
        scope.launch { listState.animateScrollToItem(history.size - 1) }
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Tasker Terminal",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MatrixGreen,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val res = app.tasker.runTask("HorizonsTest")
                    val ok = res.isSuccess
                    history.add(TermEntry("HorizonsTest", if (ok) "fired: HorizonsTest" else "error: ${res.exceptionOrNull()?.message}", ok))
                    scope.launch { if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1) }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) { Text("Fire Test Task", fontFamily = FontFamily.Monospace) }
            OutlinedButton(
                onClick = { history.clear() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MatrixGreen)
            }
        }

        if (!app.tasker.isTaskerInstalled()) {
            Text(
                "Tasker not installed — install from Play Store and enable External Access.",
                color = Color(0xFFFF6666),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history) { entry ->
                val color = if (entry.ok) MatrixGreen else Color(0xFFFF4444)
                Text(
                    "> ${entry.input}\n  ${entry.result}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = color,
                )
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Tasker task name", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        OutlinedTextField(
            value = param1,
            onValueChange = { param1 = it },
            label = { Text("param1 (optional)", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        Button(
            onClick = ::dispatch,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MatrixGreen.copy(alpha = 0.15f),
                contentColor = MatrixGreen,
            ),
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dispatch task", fontFamily = FontFamily.Monospace)
        }
    }
}
