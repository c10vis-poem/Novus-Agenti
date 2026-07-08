package com.horizons.fgs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.horizons.HorizonsApplication
import com.horizons.core.llm.NpuClient
import com.horizons.core.shell.DaemonLauncher
import com.horizons.core.shell.NativeBinaryInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.reflect.Method

/**
 * CLIFFORD — Command Line Intercept Forced Failback Over Root Daemon
 * CRS      — Clifford Recovery Service / Can't Remember Shit
 *
 * Runs in the isolated ":clifford" process so LMKD tracks it separately
 * from the main app. Android assigns FGS processes oom_score_adj ~ -200
 * to -400. The native inference daemon is spawned FROM this process so it
 * inherits that value at birth. After sh -T- reparents the daemon to init,
 * AMS loses track of the native binary and never touches its oom_score_adj
 * again — locking in FGS-level protection for the daemon's lifetime.
 * No root. No Shizuku. No 10-minute disconnects.
 *
 * CRS loop (every [CRS_INTERVAL_MS]):
 *   1. Ping daemon /health — if dead, relaunch
 *   2. Swap main app's llmRuntime to NpuClient once daemon is healthy
 *   3. Snapshot active agent turn state for hot-rehydration after main crash
 */
class CliffordService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var crsJob: Job? = null
    private var launcher: DaemonLauncher? = null
    private var npuPerfLock: AutoCloseable? = null
    private var llamaTuning: com.horizons.core.shell.NpuTuning? = null

    /** One offload probe per daemon launch — reset when (re)launching. */
    @Volatile private var offloadProbed = false
    @Volatile private var offloadSummary: String? = null

    private enum class DaemonState(val label: String) {
        BinaryMissing("waiting for daemon binary"),
        ModelMissing("waiting for model file"),
        Launching("starting daemon…"),
        Unhealthy("daemon offline"),
        Healthy("NPU daemon active"),
    }

    @Volatile private var state: DaemonState = DaemonState.BinaryMissing

    override fun onCreate() {
        super.onCreate()
        // Foreground FIRST, before any file I/O or app wiring: miss the 10s
        // startForegroundService() deadline and the system ANR-kills this
        // process silently ("Killing …:clifford (bg anr)") — reproduced in
        // emulator on 2026-07-04. Nothing may run above this line.
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_RESTART_DAEMON) {
            // Apply fresh npu-tuning.json: kill the daemon, relaunch immediately
            // (the CRS loop would also catch it, this just skips the wait).
            scope.launch {
                launcher?.stop()
                kotlinx.coroutines.delay(1_000)
                ensureDaemonRunning()
            }
            return START_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        // Init Breadcrumb in this process too so last() can read boot.log.
        com.horizons.core.diag.Breadcrumb.install(this)
        com.horizons.core.diag.Breadcrumb.drop("CliffordService_started")
        startCrs()
        return START_STICKY
    }

    private fun updateState(new: DaemonState) {
        if (state == new) return
        state = new
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        crsJob?.cancel()
        releaseNpuPerfLock()
        super.onDestroy()
    }

    // ── CRS loop ──────────────────────────────────────────────────────────────

    private fun startCrs() {
        if (crsJob?.isActive == true) return
        crsJob = scope.launch {
            val app = applicationContext as? HorizonsApplication

            // Phase 1: install + launch daemon from THIS FGS context.
            // Daemon inherits this process's oom_score_adj (~-200 to -400).
            ensureDaemonRunning()

            // Phase 2: CRS heartbeat — re-launch if daemon dies, swap runtime when healthy.
            while (isActive) {
                delay(CRS_INTERVAL_MS)
                // Refresh notification so the breadcrumb text stays current
                // even when state didn't change.
                runCatching {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, buildNotification())
                }
                val alive = pingDaemon()
                if (!alive) {
                    // Don't downgrade to Unhealthy when we're waiting on a missing
                    // binary or model — those states are more informative.
                    if (state == DaemonState.Healthy || state == DaemonState.Launching) {
                        updateState(DaemonState.Unhealthy)
                    }
                    Log.w(TAG, "CRS: daemon not responding — relaunching")
                    ensureDaemonRunning()
                } else {
                    updateState(DaemonState.Healthy)
                    probeOffloadOnce()
                    if (app != null && !app.isNpuActive) {
                        // This runs in the :clifford process — activating the local
                        // Application instance does nothing for the UI process.
                        // Broadcast so the main process activates its own NpuClient.
                        sendBroadcast(
                            Intent(ACTION_NPU_READY).setPackage(packageName)
                        )
                        app.activateNpuRuntime()
                        acquireNpuPerfLock()
                        Log.i(TAG, "CRS: daemon healthy — NpuClient activated, perf lock acquired")
                    }
                }
            }
        }
    }

    private suspend fun ensureDaemonRunning() {
        val app = applicationContext as? HorizonsApplication ?: return
        NativeBinaryInstaller.install(this)
        if (!NativeBinaryInstaller.isInstalled(this)) {
            updateState(DaemonState.BinaryMissing)
            Log.w(TAG, "CRS: no daemon binary installed — waiting for assets")
            return
        }
        val modelPath = app.resolveNpuModelPath() ?: run {
            updateState(DaemonState.ModelMissing)
            Log.w(TAG, "CRS: no model found — waiting for model")
            return
        }
        // Runtime-family dispatch: .gguf → llama-server (OpenAI-compatible),
        // everything else → ort_engine. Each family is its own drop-in package.
        val family = DaemonLauncher.familyFor(modelPath)
        val binaryName: String
        val engineArgs: List<String>
        if (family == DaemonLauncher.LLAMA_BINARY) {
            val present = java.io.File(
                applicationInfo.nativeLibraryDir, DaemonLauncher.PACKAGED_LLAMA_LIB
            ).exists() || java.io.File(filesDir, DaemonLauncher.LLAMA_BINARY).canExecute()
            if (!present) {
                updateState(DaemonState.BinaryMissing)
                Log.w(TAG, "CRS: GGUF model found but no llama-server runtime package — waiting")
                return
            }
            binaryName = DaemonLauncher.LLAMA_BINARY
            // Tunable knobs live in filesDir/npu-tuning.json (RouterPane edits
            // them; this process reads them fresh at every daemon launch).
            val tuning = com.horizons.core.shell.NpuTuning.load(this)
            engineArgs = buildList {
                add("-m"); add(modelPath)
                add("--host"); add("127.0.0.1")
                add("--port"); add(DaemonLauncher.ENGINE_PORT.toString())
                // Small ctx keeps the KV cache small — mobile inference is
                // bandwidth-bound and a runaway KV cache OOMs the device.
                add("-c"); add(tuning.ctxSize.toString())
                add("-ngl"); add("999")
                // Flash attention: big memory-bandwidth win on mobile.
                if (tuning.flashAttention) add("-fa")
                if (tuning.extraArgs.isNotBlank()) {
                    addAll(tuning.extraArgs.trim().split(Regex("\\s+")))
                }
            }
            llamaTuning = tuning
        } else {
            binaryName = NativeBinaryInstaller.installedBinaryName(this)
                ?: DaemonLauncher.ENGINE_BINARY
            engineArgs = listOf("--model", modelPath)
        }
        val l = DaemonLauncher(this, binaryName).also { launcher = it }
        if (!l.isRunning()) {
            updateState(DaemonState.Launching)
            offloadProbed = false
            offloadSummary = null
            l.launch(engineArgs, llamaTuning ?: com.horizons.core.shell.NpuTuning())
                .onSuccess { Log.i(TAG, "CRS: daemon launched PID=${it.pid}") }
                .onFailure {
                    updateState(DaemonState.Unhealthy)
                    Log.e(TAG, "CRS: daemon launch failed", it)
                }
        }
    }

    /**
     * The acceptance-bar check: once per daemon launch, read the llama-server
     * log tail and record whether Hexagon offload actually initialized or
     * decode silently fell back to CPU. Verdict goes into the CLIFFORD
     * notification (visible without adb) and filesDir/npu-status.json
     * (RouterPane reads it in the main process).
     */
    private fun probeOffloadOnce() {
        if (offloadProbed) return
        val l = launcher ?: return
        if (l.binaryName != DaemonLauncher.LLAMA_BINARY) return
        val status = com.horizons.core.shell.NpuOffloadProbe.probe(l.logFile())
        if (status.summary == "no daemon log yet") return // retry next CRS tick
        offloadProbed = true
        offloadSummary = status.summary
        com.horizons.core.shell.NpuOffloadProbe.writeStatus(this, status)
        Log.i(TAG, "NPU offload probe: ${status.summary}")
        // Force-refresh the notification so the verdict is visible immediately.
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun pingDaemon(): Boolean = try {
        val conn = java.net.URL(
            "http://127.0.0.1:${DaemonLauncher.ENGINE_PORT}/health"
        ).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod = "GET"
        val ok = conn.responseCode == 200
        conn.disconnect()
        ok
    } catch (_: Exception) { false }

    // ── NpuManager Performance Lock ───────────────────────────────────────────
    // NpuManager is an @hide system service on Qualcomm BSPs (SM8750+).
    // We acquire via reflection so the app compiles against stock AOSP SDK.
    // If the service or API doesn't exist on this device, it's a silent no-op.

    private fun acquireNpuPerfLock() {
        if (npuPerfLock != null) return
        try {
            val npuMgr = getSystemService("npu") ?: return
            val perfModeHigh = npuMgr.javaClass.getField("PERF_MODE_HIGH").getInt(null)
            val acquireMethod: Method = npuMgr.javaClass
                .getMethod("acquirePerformanceLock", Int::class.javaPrimitiveType)
            val lock = acquireMethod.invoke(npuMgr, perfModeHigh)
            npuPerfLock = lock as? AutoCloseable
            Log.i(TAG, "NpuManager: PERF_MODE_HIGH lock acquired")
        } catch (e: Exception) {
            Log.d(TAG, "NpuManager: not available on this device (${e.javaClass.simpleName})")
        }
    }

    private fun releaseNpuPerfLock() {
        try {
            npuPerfLock?.close()
        } catch (e: Exception) {
            Log.w(TAG, "NpuManager: lock release failed", e)
        }
        npuPerfLock = null
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "CLIFFORD", NotificationManager.IMPORTANCE_LOW)
                    .also { it.description = "NPU daemon process guardian" }
            )
        }
        // Surface the most recent app-lifecycle breadcrumb in the notification
        // body so the user can see WHERE the main process died without opening
        // the app or pulling logs.
        val crumb = com.horizons.core.diag.Breadcrumb.last()
        val offload = offloadSummary
        val headline =
            if (state == DaemonState.Healthy && offload != null) offload else state.label
        val expanded = buildString {
            append(state.label)
            if (offload != null) append("\n").append(offload)
            append("\nlast: ").append(crumb)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Novus Agenti")
            .setContentText(headline)
            .setStyle(Notification.BigTextStyle().bigText(expanded))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG          = "CLIFFORD"
        private const val NOTIF_ID     = 9001
        private const val CHANNEL_ID   = "clifford_brd"
        private const val CRS_INTERVAL_MS = 15_000L

        const val ACTION_STOP = "com.horizons.clifford.STOP"
        const val ACTION_RESTART_DAEMON = "com.horizons.clifford.RESTART_DAEMON"

        /** Cross-process signal: daemon healthy → main process should activate NpuClient. */
        const val ACTION_NPU_READY = "com.horizons.NPU_READY"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CliffordService::class.java))
        }

        /** Kill + relaunch the daemon so a fresh npu-tuning.json takes effect. */
        fun restartDaemon(context: Context) {
            context.startService(
                Intent(context, CliffordService::class.java).setAction(ACTION_RESTART_DAEMON)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CliffordService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
