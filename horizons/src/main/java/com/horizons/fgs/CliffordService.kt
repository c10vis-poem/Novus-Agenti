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
import com.horizons.core.llm.GenieXClient
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
    private var mediaLauncher: DaemonLauncher? = null
    private var npuPerfLock: AutoCloseable? = null

    private enum class DaemonState(val label: String) {
        BinaryMissing("waiting for daemon binary"),
        ModelMissing("waiting for model file"),
        Launching("starting daemon…"),
        Loading("loading model…"),          // process alive, /health 503 (loading or load-failed)
        Unhealthy("daemon offline"),
        Healthy("NPU daemon active"),
        Failed("daemon unavailable — import a model to retry"), // gave up relaunching
    }

    @Volatile private var state: DaemonState = DaemonState.BinaryMissing

    // ── Relaunch backoff + failure cap ─────────────────────────────────────────
    // Defense-in-depth against a genuinely-crashing daemon (e.g. a native segfault
    // that the C++ load() try/catch can't catch). The daemon-side fix already makes
    // a *clean* load failure keep the process alive serving 503, which the loop below
    // treats as "alive, don't relaunch". But if the process actually dies, we relaunch
    // with exponential backoff and stop after MAX_LAUNCH_FAILURES so we never thrash.
    private var consecutiveLaunchFailures = 0
    private var lastLaunchAttemptMs = 0L
    // Signature (path:size:mtime) of the model that was live when we gave up, so we
    // can re-arm automatically once the user imports/changes the model.
    private var failedModelSig: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Init Breadcrumb in this process too so last() can read boot.log.
        com.horizons.core.diag.Breadcrumb.install(this)
        com.horizons.core.diag.Breadcrumb.drop("CliffordService_started")
        startForeground(NOTIF_ID, buildNotification())
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

            // Phase 0: pull any recognized runtime files straight from
            // Downloads into filesDir — no "Open with" step required.
            runCatching { com.horizons.core.shell.AutoImport.sync(this@CliffordService) }

            // Phase 1: install + launch daemons from THIS FGS context.
            // They inherit this process's oom_score_adj (~-200 to -400).
            ensureDaemonRunning()
            ensureMediaDaemonRunning()
            ensureGenieDaemonRunning()

            // Phase 2: CRS heartbeat.
            // Key rule that breaks the crash loop: a LIVE process is NEVER relaunched,
            // no matter what /health says. Readiness (200 vs 503) only decides whether
            // we activate NpuClient — it never triggers a relaunch. Only an actually
            // DEAD process is relaunched, and that path has backoff + a failure cap.
            while (isActive) {
                delay(CRS_INTERVAL_MS)
                runCatching {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, buildNotification())
                }

                runCatching { com.horizons.core.shell.AutoImport.sync(this@CliffordService) }

                // Media + GenieX daemons are both optional and serve-first (503s
                // over a bad/missing model, never crash-loops) — just make sure
                // they're up if their binaries exist. GenieX readiness also
                // needs polling here (unlike media_daemon, its 200 activates a
                // runtime, so llmRuntime picks it up over the cloud/fallback).
                ensureMediaDaemonRunning()
                ensureGenieDaemonRunning()
                if (genieDaemonRunning() && app != null && !app.isGenieXActive) {
                    if (geniexModelsReady()) {
                        app.activateGenieXRuntime()
                        Log.i(TAG, "CRS: GenieX daemon healthy — GenieXClient activated")
                    }
                }

                // Auto-report main-process crashes to GitHub (this process
                // survives them). No-op without a stored GitHub token; each
                // crash uploads once. See core/diag/CrashReporter.
                runCatching {
                    com.horizons.core.diag.CrashReporter.maybeUpload(
                        this@CliffordService,
                        app?.appState?.get(com.horizons.core.state.AppStateStore.KEY_GITHUB_TOKEN),
                    )
                }

                // Terminal state: we gave up relaunching. Sit idle until the model
                // changes (user imported a new/valid one), then re-arm.
                if (state == DaemonState.Failed) {
                    if (modelSignature(app) != failedModelSig) {
                        Log.i(TAG, "CRS: model changed since failure — re-arming daemon")
                        resetFailureState()
                        ensureDaemonRunning()
                    }
                    continue
                }

                val processAlive = launcher?.isRunning() == true

                if (processAlive) {
                    // Process is up. Readiness decides activation; we do NOT relaunch.
                    consecutiveLaunchFailures = 0
                    when (daemonHealthCode()) {
                        200 -> {
                            updateState(DaemonState.Healthy)
                            if (app != null && !app.isNpuActive) {
                                app.activateNpuRuntime()
                                acquireNpuPerfLock()
                                Log.i(TAG, "CRS: daemon healthy — NpuClient activated, perf lock acquired")
                            }
                        }
                        // 503 = alive but model still loading OR load failed. Either way the
                        // daemon is up and stable — show progress, never relaunch. No thrash.
                        503  -> updateState(DaemonState.Loading)
                        else -> updateState(DaemonState.Unhealthy)
                    }
                } else {
                    // Process is genuinely dead.
                    if (state == DaemonState.BinaryMissing || state == DaemonState.ModelMissing) {
                        // Nothing to launch yet — cheap re-check in case assets/model
                        // arrived. Not a launch failure, so don't count it.
                        ensureDaemonRunning()
                        continue
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastLaunchAttemptMs < launchBackoffMs()) continue
                    if (consecutiveLaunchFailures >= MAX_LAUNCH_FAILURES) {
                        failedModelSig = modelSignature(app)
                        updateState(DaemonState.Failed)
                        Log.e(TAG, "CRS: daemon died $consecutiveLaunchFailures× — giving up until model changes")
                        continue
                    }
                    consecutiveLaunchFailures++
                    lastLaunchAttemptMs = now
                    Log.w(TAG, "CRS: daemon dead — relaunch attempt $consecutiveLaunchFailures/$MAX_LAUNCH_FAILURES")
                    ensureDaemonRunning()
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
        val binaryName = NativeBinaryInstaller.installedBinaryName(this)
            ?: DaemonLauncher.ENGINE_BINARY
        val l = DaemonLauncher(this, binaryName).also { launcher = it }
        if (!l.isRunning()) {
            updateState(DaemonState.Launching)
            l.launch(listOf("--model", modelPath))
                .onSuccess { Log.i(TAG, "CRS: daemon launched PID=${it.pid}") }
                .onFailure {
                    updateState(DaemonState.Unhealthy)
                    Log.e(TAG, "CRS: daemon launch failed", it)
                }
        }
    }

    /**
     * Launch the media (STT/TTS) daemon if its binary has been imported and it
     * isn't already running. Deliberately simpler than the model daemon's state
     * machine: media_daemon is serve-first (binds :8091 immediately, 503s until
     * models load, never exits over a bad model), so process liveness is the
     * only thing to guard. Model dirs are discovered by marker file —
     * preprocess.onnx = Moonshine, voices.bin = Kokoro — under the app's
     * models/ dir first, then /Download.
     */
    private suspend fun ensureMediaDaemonRunning() {
        val binary = java.io.File(filesDir, MEDIA_DAEMON_BINARY)
        if (!binary.canExecute()) return  // not imported yet — optional daemon
        val l = mediaLauncher ?: DaemonLauncher(this, MEDIA_DAEMON_BINARY)
            .also { mediaLauncher = it }
        if (l.isRunning()) return

        fun findModelDir(marker: String): String? {
            val roots = listOf(
                java.io.File(filesDir, "models"),
                java.io.File("/storage/emulated/0/Download"),
            )
            for (root in roots) {
                if (java.io.File(root, marker).canRead()) return root.absolutePath
                root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                    if (java.io.File(dir, marker).canRead()) return dir.absolutePath
                }
            }
            return null
        }

        val args = mutableListOf("--port", DaemonLauncher.MEDIA_PORT.toString())
        findModelDir("preprocess.onnx")?.let { args += listOf("--stt-dir", it) }
        findModelDir("voices.bin")?.let { args += listOf("--tts-dir", it) }

        // Its own ONNX Runtime copy (version-pinned separately from
        // ort_engine's — see RuntimeFiles.MEDIA_ONNXRUNTIME_ARTIFACT) must be
        // searched BEFORE filesDir root, or the mismatched shared copy wins.
        val mediaLibsDir = java.io.File(filesDir, com.horizons.core.shell.RuntimeFiles.MEDIA_LIBS_SUBDIR)
        l.launch(args, extraLibDirs = listOf(mediaLibsDir.absolutePath))
            .onSuccess { Log.i(TAG, "CRS: media daemon launched PID=${it.pid} args=$args") }
            .onFailure { Log.w(TAG, "CRS: media daemon launch failed", it) }
    }

    // ── GenieX daemon (:18181, OpenAI wire — see GenieXClient) ─────────────────
    // Deliberately simpler than the legacy ort_engine state machine above:
    // serve-first means liveness is the only thing to guard (no relaunch
    // backoff needed — a bad model just means /v1/models never returns 200,
    // which the CRS loop above polls for before activating GenieXClient).
    //
    // HARD RULE (operator, session 17): this daemon NEVER launches on its
    // own just because a binary + GGUF happen to be present. It launches
    // ONLY in response to AppStateStore.KEY_ACTIVE_GENIEX_MODEL being set —
    // i.e. the operator explicitly picking a model in the Router UI. Ship
    // empty, land empty, run empty, loaded on the operator's end. Picking a
    // DIFFERENT model while one is already running stops the old process
    // and relaunches with the new one — that's the whole hot-swap story for
    // now (geniex_daemon loads one model per process; true per-request
    // multi-model serving, which GenieX's real CLI supports, is a future
    // upgrade, not required for "swap without a rebuild").

    private var genieLauncher: DaemonLauncher? = null
    private var genieLoadedModelPath: String? = null

    private fun genieDaemonRunning(): Boolean = genieLauncher?.isRunning() == true

    private suspend fun ensureGenieDaemonRunning() {
        val app = applicationContext as? HorizonsApplication ?: return
        val binary = java.io.File(filesDir, GENIEX_DAEMON_BINARY)
        if (!binary.canExecute()) return  // not imported yet

        val selected = app.activeGenieXModelPath
        val l = genieLauncher ?: DaemonLauncher(this, GENIEX_DAEMON_BINARY).also { genieLauncher = it }

        if (selected == null) {
            // Nothing selected (or the operator cleared the selection) — if
            // a daemon happens to be running from a prior selection, stop it.
            if (l.isRunning()) {
                Log.i(TAG, "CRS: GenieX model deselected — stopping daemon")
                l.stop()
                genieLoadedModelPath = null
            }
            return
        }

        if (l.isRunning() && selected == genieLoadedModelPath) return  // already serving the right model

        if (l.isRunning()) {
            Log.i(TAG, "CRS: GenieX model swap $genieLoadedModelPath -> $selected — restarting daemon")
            l.stop()
            // Give the port a moment to free before rebinding.
            kotlinx.coroutines.delay(500)
        }

        val pluginDir = java.io.File(java.io.File(filesDir, "geniex-plugins"), "plugins")
        if (!pluginDir.isDirectory) {
            Log.w(TAG, "CRS: GenieX plugins not extracted yet (import geniex-plugins-arm64.tar.gz) — waiting")
            return
        }

        val args = listOf(
            "--port", GenieXClient.DEFAULT_PORT.toString(),
            "--model", selected,
            "--plugin", "llama_cpp",
            "--device", "npu",
            "--plugin-dir", pluginDir.absolutePath,
            "--model-name", java.io.File(selected).name,
        )
        l.launch(args)
            .onSuccess {
                genieLoadedModelPath = selected
                Log.i(TAG, "CRS: GenieX daemon launched PID=${it.pid} model=$selected")
            }
            .onFailure { Log.w(TAG, "CRS: GenieX daemon launch failed", it) }
    }

    /** GET :18181/v1/models — 200 + non-empty data[] means a model is loaded. */
    private fun geniexModelsReady(): Boolean = try {
        val conn = java.net.URL(
            "http://127.0.0.1:${GenieXClient.DEFAULT_PORT}/v1/models"
        ).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod = "GET"
        val ready = conn.responseCode == 200
        conn.disconnect()
        ready
    } catch (_: Exception) { false }

    /**
     * HTTP status from the daemon's /health, or -1 if the port didn't respond at all.
     * 200 = ready · 503 = alive but model not ready (loading or load-failed) · -1 = unreachable.
     * Note: readiness is only used to decide activation. Whether to relaunch is decided
     * by process liveness (launcher.isRunning()), never by this code.
     */
    private fun daemonHealthCode(): Int = try {
        val conn = java.net.URL(
            "http://127.0.0.1:${DaemonLauncher.ENGINE_PORT}/health"
        ).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod = "GET"
        val code = conn.responseCode
        conn.disconnect()
        code
    } catch (_: Exception) { -1 }

    /** Exponential backoff between relaunch attempts: 15s, 30s, 60s, 120s, capped at 4min. */
    private fun launchBackoffMs(): Long =
        (CRS_INTERVAL_MS shl consecutiveLaunchFailures).coerceAtMost(240_000L)

    private fun resetFailureState() {
        consecutiveLaunchFailures = 0
        lastLaunchAttemptMs = 0L
        failedModelSig = null
    }

    /** path:size:mtime of the currently-resolvable model, or null if none. Used to
     *  detect that the user imported/changed the model so we can re-arm after giving up. */
    private fun modelSignature(app: HorizonsApplication?): String? {
        val path = app?.resolveNpuModelPath() ?: return null
        val f = java.io.File(path)
        return "$path:${f.length()}:${f.lastModified()}"
    }

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
        val expanded = "${state.label}\nlast: $crumb"
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Novus Agenti")
            .setContentText(state.label)
            .setStyle(Notification.BigTextStyle().bigText(expanded))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG          = "CLIFFORD"
        private const val NOTIF_ID     = 9001
        private const val CHANNEL_ID   = "clifford_brd"
        private const val CRS_INTERVAL_MS = 15_000L
        // Consecutive dead-process relaunches before we give up and idle in Failed
        // state (re-armed automatically when the model file changes).
        private const val MAX_LAUNCH_FAILURES = 5

        private const val MEDIA_DAEMON_BINARY = "media_daemon"
        private const val GENIEX_DAEMON_BINARY = "geniex_daemon"

        const val ACTION_STOP = "com.horizons.clifford.STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CliffordService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CliffordService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
