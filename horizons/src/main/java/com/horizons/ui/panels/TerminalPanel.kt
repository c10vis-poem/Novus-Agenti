package com.horizons.ui.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.horizons.HorizonsApplication
import kotlinx.coroutines.launch

private data class ShellEntry(val cmd: String, val stdout: String, val stderr: String, val exitCode: Int)
private data class TermEntry(val input: String, val result: String, val ok: Boolean)

@Composable
fun TerminalPanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Shell") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Tasker") },
            )
        }

        when (selectedTab) {
            0 -> ShellTab(app = app, scope = scope)
            1 -> TaskerTab(app = app, scope = scope)
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
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Termux not installed — install from F-Droid and grant RUN_COMMAND permission.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        // History — newest at bottom, oldest scrolled off top (real terminal behaviour)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history.asReversed()) { entry ->
                val isError = entry.exitCode != 0
                val color = if (isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                val output = when {
                    entry.stdout.isNotEmpty() && entry.stderr.isNotEmpty() ->
                        "${entry.stdout}\n[stderr] ${entry.stderr}"
                    entry.stdout.isNotEmpty() -> entry.stdout
                    entry.stderr.isNotEmpty() -> entry.stderr
                    else -> "(no output, exit ${entry.exitCode})"
                }
                Text(
                    "$ ${entry.cmd}\n$output",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = color,
                )
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = cmd,
            onValueChange = { cmd = it },
            label = { Text("Shell command") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !running,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { runCmd() }),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = ::runCmd,
                enabled = !running && cmd.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running…")
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run")
                }
            }
            OutlinedButton(onClick = { history.clear() }) {
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
        Text("Tasker Terminal", style = MaterialTheme.typography.titleMedium)

        // Quick smoke-test buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val res = app.tasker.runTask("HorizonsTest")
                val ok = res.isSuccess
                history.add(TermEntry("HorizonsTest", if (ok) "fired: HorizonsTest" else "error: ${res.exceptionOrNull()?.message}", ok))
                scope.launch { if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1) }
            }) { Text("Fire Test Task") }
            OutlinedButton(onClick = { history.clear() }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear")
            }
        }

        if (!app.tasker.isTaskerInstalled()) {
            Text(
                "Tasker not installed — install from Play Store and enable External Access (Tasker → Preferences → Misc).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // History
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history) { entry ->
                val color = if (entry.ok) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error
                Text(
                    "> ${entry.input}\n  ${entry.result}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = color,
                )
            }
        }

        HorizontalDivider()

        // Input
        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Tasker task name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        OutlinedTextField(
            value = param1,
            onValueChange = { param1 = it },
            label = { Text("param1 (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        Button(onClick = ::dispatch, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dispatch task")
        }
    }
}
