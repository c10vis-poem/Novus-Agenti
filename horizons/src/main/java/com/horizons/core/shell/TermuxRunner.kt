package com.horizons.core.shell

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs commands inside Termux's environment (login shell — full $PREFIX PATH,
 * so `openwiki`, `node`, `python`, `git` etc. all resolve) via Termux's
 * RUN_COMMAND service, and returns stdout/stderr/exit code.
 *
 * This is the piece TaskerBridge deliberately avoided: the historical failure
 * mode was that a crashed / permission-less Termux never fires the result
 * intent, hanging the caller forever. Fixed here with a hard timeout — the
 * await is wrapped in withTimeoutOrNull, so the worst case is a clean error
 * row in the terminal, never a hang. Both shells now coexist in the app:
 * TaskerBridge.runShellCommand = the app-sandbox shell, this = Termux.
 *
 * Device-side prerequisites (one-time, surfaced in the Terminal UI):
 *   1. Termux installed (F-Droid build).
 *   2. `allow-external-apps = true` in ~/.termux/termux.properties
 *      (then restart Termux).
 *   3. The RUN_COMMAND permission is granted to this app (manifest declares
 *      it; Android grants it at install since it's a normal-level permission).
 *
 * Result delivery: Termux fires our mutable PendingIntent with a "result"
 * bundle (stdout/stderr/exitCode/errmsg) — received by a dynamically
 * registered, non-exported receiver keyed by a unique execution id.
 */
object TermuxRunner {

    private const val TAG = "TermuxRunner"

    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"

    private const val RESULT_ACTION_PREFIX = "com.horizons.TERMUX_RESULT_"

    private val nextId = AtomicInteger(1)

    /**
     * Execute [command] via `bash -lc` in Termux. Never hangs: resolves to an
     * error result after [timeoutMs] if Termux doesn't answer.
     */
    suspend fun run(
        context: Context,
        command: String,
        timeoutMs: Long = 30_000L,
    ): TaskerBridge.ShellResult {
        val app = context.applicationContext
        val id = nextId.getAndIncrement()
        val action = "$RESULT_ACTION_PREFIX$id"
        val deferred = CompletableDeferred<TaskerBridge.ShellResult>()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val result = intent?.getBundleExtra("result")
                val stdout = result?.getString("stdout") ?: ""
                val stderr = result?.getString("stderr") ?: ""
                val exit = result?.getInt("exitCode", -1) ?: -1
                val errmsg = result?.getString("errmsg") ?: ""
                deferred.complete(
                    TaskerBridge.ShellResult(
                        exitCode = exit,
                        stdout = stdout.trimEnd(),
                        stderr = listOf(stderr.trimEnd(), errmsg.trim())
                            .filter { it.isNotEmpty() }.joinToString("\n"),
                    )
                )
            }
        }

        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(receiver, filter)
        }

        try {
            // Explicit intent to our own package so the non-exported receiver
            // still gets it; MUTABLE so Termux can attach the result bundle.
            val resultIntent = Intent(action).setPackage(app.packageName)
            val pi = PendingIntent.getBroadcast(
                app, id, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )

            val svc = Intent().apply {
                setClassName(TaskerBridge.TERMUX_PACKAGE, TERMUX_SERVICE)
                setAction(ACTION_RUN_COMMAND)
                putExtra(EXTRA_PATH, TERMUX_BASH)
                putExtra(EXTRA_ARGS, arrayOf("-lc", command))
                putExtra(EXTRA_WORKDIR, TERMUX_HOME)
                putExtra(EXTRA_BACKGROUND, true)
                putExtra(EXTRA_PENDING_INTENT, pi)
            }
            try {
                app.startService(svc)
            } catch (e: Exception) {
                // e.g. SecurityException when allow-external-apps is off, or
                // background-start restrictions. Fail fast with the reason.
                return TaskerBridge.ShellResult(
                    126, "",
                    "Termux launch failed: ${e.message}\n" +
                        "Check: Termux installed + allow-external-apps=true in " +
                        "~/.termux/termux.properties (then restart Termux).",
                )
            }

            return withTimeoutOrNull(timeoutMs) { deferred.await() }
                ?: TaskerBridge.ShellResult(
                    124, "",
                    "Termux did not answer within ${timeoutMs / 1000}s. " +
                        "If this repeats: open Termux once so it's running, and verify " +
                        "allow-external-apps=true in ~/.termux/termux.properties.",
                )
        } finally {
            try { app.unregisterReceiver(receiver) } catch (_: Exception) {}
            Log.d(TAG, "run#$id finished")
        }
    }
}
