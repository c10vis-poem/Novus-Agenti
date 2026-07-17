package com.horizons

import android.app.ActivityManager
import android.app.Application
import android.os.Process
import com.horizons.audio.AudioRecorder
import com.horizons.audio.VadFactory
import com.horizons.audio.VoiceLoopController
import com.horizons.core.llm.CloudLlmRuntime
import com.horizons.core.llm.GenieXClient
import com.horizons.core.llm.LlmRuntime
import com.horizons.core.llm.NpuClient
import com.horizons.core.log.CrashRecorder
import com.horizons.core.perf.GameModeBoost
import com.horizons.core.perf.GameModeBoost.gameBoosted
import com.horizons.core.agent.AgentLoop
import com.horizons.core.shell.TaskerBridge
import com.horizons.core.state.AppStateStore
import com.horizons.core.state.ChatHistoryStore
import com.horizons.core.state.SavedCommandStore
import com.horizons.core.stt.DaemonSttClient
import com.horizons.core.voice.KokoroModelManager
import com.horizons.core.voice.SherpaOnnxTtsClient
import com.horizons.provider.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ChatMessage(val role: String, val text: String)

/** Three operating modes for continuous vs. one-shot AI interaction. */
enum class ChatMode {
    /** Mode A — Live Screen Share: continuous MediaProjection + audio loop + LLM stream (FGS). */
    A,
    /** Mode B — Live Chat: audio loop + LLM stream, no screen capture (FGS). */
    B,
    /** Mode C — One-shot: manual attach or dock capture; single LLM call. Default. */
    C,
}

/**
 * Application singleton. All AI inference runs on-device via
 * Qwen3.5-9B via ort_engine daemon (Hexagon HTP v79).
 * TTS: Sherpa-ONNX -> Kokoro multi-lang v1.0 (28 English voices), no Android TTS broker.
 */
class HorizonsApplication : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appState: AppStateStore
        private set

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val chatHistory: ChatHistoryStore by lazy { ChatHistoryStore(this) }
    val savedCommands: SavedCommandStore by lazy { SavedCommandStore(this) }
    val tasker: TaskerBridge by lazy { TaskerBridge(this) }

    // -- Agentic loop -- LLM + full Android API tool registry --
    // Uses a lambda provider so swapping llmRuntime is transparent.
    val agentLoop: AgentLoop by lazy { AgentLoop(this, { llmRuntime }, tasker, appState) }

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    // -- Kokoro model manager -- downloads & extracts the TTS model on first run --
    val kokoroManager: KokoroModelManager by lazy { KokoroModelManager(this, scope) }

    // -- Sherpa-ONNX TTS (Kokoro voices, no Android TextToSpeech broker) --
    val tts: SherpaOnnxTtsClient by lazy { SherpaOnnxTtsClient(kokoroManager.modelDir) }

    // -- Persisted voice settings exposed for RouterPane --
    val ttsVoiceId: MutableStateFlow<String> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_VOICE) ?: SherpaOnnxTtsClient.DEFAULT_VOICE)
    }
    val ttsSpeed: MutableStateFlow<Float> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_SPEED)?.toFloatOrNull() ?: 1.0f)
    }

    // -- Shared AudioRecorder --
    val audioRecorder: AudioRecorder by lazy { AudioRecorder(this) }

    // -- STT via the media daemon (Whisper) -- never in-process; models run detached --
    val stt: DaemonSttClient by lazy { DaemonSttClient(appState) }

    // -- Pending screen capture (dock button -> stored here -> chat ask) --
    val pendingScreenJpeg = MutableStateFlow<ByteArray?>(null)

    // -- LLM runtimes -- local daemons first, cloud fallback --
    // GenieX is checked before the legacy ort_engine NpuClient: it's the
    // decided primary runtime (wiki/GENIEX-DAEMON-PLAN.md) and the only one
    // that can load GGUF, which is what actually sits in /Download today.
    @Volatile private var _genieXClient: GenieXClient? = null
    @Volatile private var _npuClient: NpuClient? = null
    val cloudRuntime: CloudLlmRuntime by lazy { CloudLlmRuntime(appState) }

    val llmRuntime: LlmRuntime get() {
        _genieXClient?.let { return it }
        _npuClient?.let { return it }
        if (cloudRuntime.isConfigured) return cloudRuntime
        return _fallbackRuntime
    }

    val isGenieXActive: Boolean get() = _genieXClient != null
    val isNpuActive: Boolean get() = _npuClient != null

    private val _fallbackRuntime = object : LlmRuntime {
        // Deliberately does NOT start with "Adreno 830" or "Hexagon HTP" —
        // those prefixes are the app-wide "a real backend is ready" signal
        // (see LlmRuntime.backendStatus doc). This text used to be
        // "Adreno 830 · no backend", which impersonated that exact signal —
        // every ready-check in the app (HomeGrid, RouterPane) showed a false
        // green "ACTIVE" badge with zero backend running (operator-caught).
        override val backendStatus = MutableStateFlow("Offline · no backend configured")
        override fun stream(prompt: String) = flow<String> {
            emit("[No inference backend available — start the on-device daemon or add a cloud API key in Settings]")
        }
    }

    // -- Chat mode (A/B/C) -- updated by FGS services on start/stop --
    val chatMode = MutableStateFlow(ChatMode.C)

    // -- Voice loop (Mode B) --
    val voiceLoop: VoiceLoopController by lazy {
        VoiceLoopController(
            scope = scope,
            recorder = audioRecorder,
            tts = tts,
            engineStreamAudio = { pcm ->
                // VAD → media-daemon STT (Whisper) → LLM (text reasoning) → TTS.
                // STT runs in the daemon, not here; the LLM only ever sees text.
                flow {
                    val text = transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
                    if (text.isNotBlank()) emitAll(llmRuntime.stream(text))
                }.gameBoosted(this@HorizonsApplication)
            },
            vad = VadFactory.create(this@HorizonsApplication),
        )
    }

    // -- Model test (Diagnostics tab) --
    val modelTestResult = MutableStateFlow<String?>(null)

    fun testModel() {
        if (_chatBusy.value) return
        modelTestResult.value = "testing..."
        scope.launch {
            var out = ""
            llmRuntime.stream("Say hi in one word.").collect { out += it }
            modelTestResult.value = "backend: ${llmRuntime.backendStatus.value}\n\n" +
                out.take(300).ifBlank { "(empty response)" }
        }
    }

    // -- Chat state --
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

    private var _activeSessionId: String = java.util.UUID.randomUUID().toString()
    val activeSessionId: String get() = _activeSessionId
    private var _activeSessionMode: String = "standard"

    fun newChatSession() {
        saveCurrentSession()
        _chatMessages.value = emptyList()
        _activeSessionId = java.util.UUID.randomUUID().toString()
        _activeSessionMode = "standard"
    }

    fun loadSession(sessionId: String) {
        saveCurrentSession()
        val session = chatHistory.getSession(sessionId) ?: return
        _chatMessages.value = session.messages
        _activeSessionId = session.id
        _activeSessionMode = session.mode
    }

    fun saveCurrentSession() {
        val msgs = _chatMessages.value
        if (msgs.isEmpty()) return
        scope.launch {
            chatHistory.save(
                com.horizons.core.state.ChatSession(
                    id = _activeSessionId,
                    messages = msgs,
                    mode = _activeSessionMode,
                )
            )
        }
    }

    fun setSessionMode(mode: String) { _activeSessionMode = mode }

    fun sendChat(prompt: String) {
        if (_chatBusy.value) return
        _chatMessages.value = _chatMessages.value + ChatMessage("user", prompt)
        _chatBusy.value = true
        GameModeBoost.enterHotLoop(this)
        scope.launch {
            val assistantIdx = _chatMessages.value.size
            _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "")
            var reply = ""
            var lastNanos = System.nanoTime()
            try {
                llmRuntime.stream(prompt).collect { chunk ->
                    val now = System.nanoTime()
                    GameModeBoost.reportWork(now - lastNanos)
                    lastNanos = now
                    reply += chunk
                    val list = _chatMessages.value.toMutableList()
                    list[assistantIdx] = ChatMessage("assistant", reply)
                    _chatMessages.value = list
                }
            } finally {
                _chatBusy.value = false
                GameModeBoost.exitHotLoop(this@HorizonsApplication)
                saveCurrentSession()
            }
        }
    }

    fun stopAll() {
        tts.stop()
        voiceLoop.stop()
        _chatBusy.value = false
    }

    /** Screen Q&A: JPEG -> Qwen3.5-9B vision (ort_engine) -> streaming answer in chat. */
    fun screenAsk(jpegBytes: ByteArray, question: String) {
        if (_chatBusy.value) return
        val q = question.ifBlank { "What is on this screen?" }
        _chatMessages.value = _chatMessages.value + ChatMessage("user", "[Screen] $q")
        _chatBusy.value = true
        GameModeBoost.enterHotLoop(this)
        scope.launch {
            val assistantIdx = _chatMessages.value.size
            _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "")
            var reply = ""
            try {
                llmRuntime.streamImage(jpegBytes, q).collect { chunk ->
                    reply += chunk
                    val list = _chatMessages.value.toMutableList()
                    list[assistantIdx] = ChatMessage("assistant", reply)
                    _chatMessages.value = list
                }
                if (reply.isNotBlank()) scope.launch { tts.speak(reply.stripForTts()) }
            } finally {
                _chatBusy.value = false
                GameModeBoost.exitHotLoop(this@HorizonsApplication)
                saveCurrentSession()
            }
        }
    }

    /**
     * Voice STT: PCM -> media daemon (Whisper), never in-process, model-independent.
     * Falls back to the active LLM's audio path only if the media daemon is down
     * and a model that accepts audio is loaded.
     */
    suspend fun transcribeAudio(pcm: ShortArray, sampleRate: Int): String {
        // Media daemon (Whisper) first; returns "" if the daemon isn't reachable.
        val text = stt.transcribe(pcm, sampleRate)
        if (text.isNotBlank()) return text
        // Fallback only if the media daemon is down and an audio-capable LLM is active.
        val wav = pcmToWav(pcm, sampleRate)
        var result = ""
        llmRuntime.streamAudio(wav, "Transcribe the audio accurately.").collect { result += it }
        return result.trim()
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return true
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }
            ?.processName == packageName
    }

    override fun onCreate() {
        com.horizons.core.diag.Breadcrumb.install(this)
        com.horizons.core.diag.Breadcrumb.drop("onCreate_enter")
        super.onCreate()
        com.horizons.core.diag.Breadcrumb.drop("onCreate_after_super")

        if (!isMainProcess()) {
            com.horizons.core.diag.Breadcrumb.drop("onCreate_skip_non_main_process")
            appState = AppStateStore(this)
            return
        }

        try {
            CrashRecorder(this).install()
            com.horizons.core.diag.Breadcrumb.drop("crashrecorder_installed")

            appState = AppStateStore(this)
            com.horizons.core.diag.Breadcrumb.drop("appstate_loaded")

            // CLIFFORD FGS -- separate process. Failure here shouldn't kill main.
            try {
                com.horizons.fgs.CliffordService.start(this)
                com.horizons.core.diag.Breadcrumb.drop("clifford_started")
            } catch (e: Throwable) {
                com.horizons.core.diag.Breadcrumb.drop("clifford_failed: ${e.javaClass.simpleName}: ${e.message}")
            }

            try {
                cloudRuntime.refreshStatus()
                com.horizons.core.diag.Breadcrumb.drop("cloud_refreshed")
            } catch (e: Throwable) {
                com.horizons.core.diag.Breadcrumb.drop("cloud_refresh_failed: ${e.javaClass.simpleName}: ${e.message}")
            }

            // In-process Kokoro/Sherpa TTS is DELIBERATELY NOT initialized at
            // boot anymore. OfflineTts() is a native (JNI) constructor — if it
            // aborts, it takes the whole process down with no Java stack trace,
            // which is exactly the crash-on-launch shipped in session 17's
            // first APK (first boot: ~20s model download → native init → die;
            // every boot after: files present → die in ~1s). It also violates
            // the "no in-process tensor runtime" hard rule. Speech synthesis
            // belongs to media_daemon (:8091, DaemonTtsClient). tts.speak() is
            // null-safe and simply no-ops until something explicitly inits it.
            com.horizons.core.diag.Breadcrumb.drop("tts_boot_init_skipped_by_design")
            tts.voiceId = ttsVoiceId.value
            tts.speed   = ttsSpeed.value

            // -- STT: probe the media daemon so stt.ready reflects connectivity --
            scope.launch { runCatching { stt.probe() } }

            // -- Runtime watcher: THE activation path for local daemons --
            // CliffordService (:clifford process) launches/guards the native
            // daemons but CANNOT activate runtimes here — it lives across a
            // process boundary and only ever mutated its own dead copy of
            // this singleton (root cause of "daemon healthy, chat still says
            // no backend"). This loop runs in THE UI PROCESS: it polls the
            // daemons' local ports and flips llmRuntime accordingly, both
            // directions (activate on ready, deactivate on gone/deselected).
            scope.launch {
                while (true) {
                    runCatching {
                        val genieSelected = java.io.File(filesDir, ACTIVE_GENIEX_MODEL_FILE).canRead()
                        val genieReady = genieSelected && probeOk(
                            "http://127.0.0.1:${com.horizons.core.llm.GenieXClient.DEFAULT_PORT}/v1/models")
                        if (genieReady && _genieXClient == null) {
                            activateGenieXRuntime()
                            com.horizons.core.diag.Breadcrumb.drop("geniex_runtime_activated")
                        } else if (!genieReady && _genieXClient != null) {
                            _genieXClient = null
                            com.horizons.core.diag.Breadcrumb.drop("geniex_runtime_deactivated")
                        }

                        val ortReady = probeOk(
                            "http://127.0.0.1:${com.horizons.core.shell.DaemonLauncher.ENGINE_PORT}/health")
                        if (ortReady && _npuClient == null) {
                            activateNpuRuntime()
                            com.horizons.core.diag.Breadcrumb.drop("npu_runtime_activated")
                        } else if (!ortReady && _npuClient != null) {
                            _npuClient = null
                        }
                    }
                    kotlinx.coroutines.delay(5_000)
                }
            }

            scope.launch { ttsVoiceId.collect { id -> tts.voiceId = id; appState.put(AppStateStore.KEY_TTS_VOICE, id) } }
            scope.launch { ttsSpeed.collect  { sp -> tts.speed   = sp; appState.put(AppStateStore.KEY_TTS_SPEED, sp.toString()) } }

            com.horizons.core.diag.Breadcrumb.drop("onCreate_exit_ok")
        } catch (e: Throwable) {
            com.horizons.core.diag.Breadcrumb.drop("onCreate_threw: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    /** True while any local (on-device) LLM daemon is serving the chat. */
    private fun probeOk(url: String): Boolean = try {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 1_000
        conn.readTimeout = 1_500
        conn.requestMethod = "GET"
        val ok = conn.responseCode == 200
        conn.disconnect()
        ok
    } catch (_: Exception) { false }

    fun activateNpuRuntime() {
        if (_npuClient == null) _npuClient = NpuClient()
    }

    fun activateGenieXRuntime() {
        if (_genieXClient == null) _genieXClient = GenieXClient()
    }

    /**
     * All GGUFs available to load via GenieX (format-specific: GenieX's
     * llama_cpp plugin needs GGUF, unlike [resolveNpuModelPath]'s ort_engine
     * target, which needs qnn_context_binary/.onnx and can't read GGUF at
     * all). Purely a discovery list for the Router picker UI — NOTHING here
     * auto-launches a daemon. The operator selects one explicitly, which
     * writes AppStateStore.KEY_ACTIVE_GENIEX_MODEL; CliffordService only
     * ever launches/reloads geniex_daemon in response to that key changing.
     * (Was auto-pick-and-launch-on-file-presence — operator hard-reject,
     * session 17: "ship empty, land empty, run empty, loaded on my end.")
     */
    fun listGenieXModelCandidates(): List<java.io.File> {
        val modelsDir = java.io.File(filesDir, "models")
        val roots = listOf(modelsDir, java.io.File("/storage/emulated/0/Download"))
        return roots.asSequence()
            .flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
            .filter { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) }
            .sortedByDescending { it.length() }
            .toList()
    }

    /** Currently-selected GenieX model, or null if the operator hasn't chosen
     *  one. Reads the cross-process file (the source of truth CliffordService
     *  acts on), not the prefs copy. */
    val activeGenieXModelPath: String?
        get() = java.io.File(filesDir, ACTIVE_GENIEX_MODEL_FILE)
            .takeIf { it.canRead() }?.readText()?.trim()?.takeIf { it.isNotBlank() }

    /** Selects (or clears, if null) the model GenieX should load. Swapping models — or
     *  unloading entirely — is just calling this again; CliffordService does the rest. */
    fun setActiveGenieXModel(path: String?) {
        appState.put(AppStateStore.KEY_ACTIVE_GENIEX_MODEL, path ?: "")
        // CROSS-PROCESS: CliffordService runs in :clifford, a SEPARATE process,
        // and it's the thing that actually launches the daemon. It cannot see
        // this main process's AppStateStore (EncryptedSharedPreferences is not
        // multi-process coherent — the daemon-launcher was reading a stale
        // empty selection forever, which is why "Load" did nothing and the app
        // stayed on "no backend"). A plain file in filesDir is the reliable
        // cross-process channel; CliffordService reads it fresh each CRS tick.
        runCatching {
            val f = java.io.File(filesDir, ACTIVE_GENIEX_MODEL_FILE)
            if (path == null) f.delete() else f.writeText(path)
        }
        // Always reset, even when swapping (not just clearing): GenieXClient
        // caches its discovered model id for the life of the instance, so a
        // stale client would keep talking about the OLD model after the
        // daemon restarts with a new one. llmRuntime falls through to
        // cloud/fallback until CliffordService confirms the swapped daemon
        // is actually up and re-activates a fresh client.
        _genieXClient = null
    }

    fun resolveNpuModelPath(): String? {
        // Qwen3.5-9B compiled binaries (qnn_context_binary)
        val variants = listOf(
            "qwen3_5_9b_unified.bin",
            "qwen3_5_9b_language_decoder.bin",
        )
        val modelsDir = java.io.File(filesDir, "models")
        val roots = listOf(modelsDir, filesDir, java.io.File("/storage/emulated/0/Download"))
        for (root in roots) for (name in variants) {
            val f = java.io.File(root, name)
            if (f.canRead()) return f.absolutePath
        }
        // Any LLM model file imported into the app-private models dir (newest first).
        // Excludes Whisper/STT GGML files — ort_engine can't load those.
        fun isLlmCandidate(f: java.io.File): Boolean {
            val n = f.name.lowercase()
            if (!ModelImportActivity.MODEL_EXTENSIONS.any { n.endsWith(it) }) return false
            // Whisper STT variants ship as ggml-{tiny,base,small,medium,large}.bin — skip them.
            if (n.startsWith("ggml-tiny") || n.startsWith("ggml-base") ||
                n.startsWith("ggml-small") || n.startsWith("ggml-medium") ||
                n.startsWith("ggml-large") || n.contains("whisper")) return false
            // Aux/media models are not LLMs: VAD, Moonshine STT parts, Kokoro TTS.
            if (n.contains("silero") || n.contains("vad") ||
                n.contains("moonshine") || n.startsWith("preprocess") ||
                n.startsWith("encode") || n.contains("decode") ||
                n.contains("kokoro") || n == "model.onnx") return false
            return true
        }
        modelsDir.listFiles()
            ?.filter { it.isFile && isLlmCandidate(it) }
            ?.maxByOrNull { it.lastModified() }
            ?.let { return it.absolutePath }
        // Same scan in Downloads — but only pick LLM-shaped files, not Whisper.
        java.io.File("/storage/emulated/0/Download").listFiles()
            ?.filter { it.isFile && isLlmCandidate(it) }
            ?.maxByOrNull { it.lastModified() }
            ?.let { return it.absolutePath }
        return null
    }

    companion object {
        private const val TAG = "HorizonsApp"

        /** Cross-process channel for the operator's explicit GenieX model
         *  selection (main UI process writes, :clifford reads). Plain file
         *  because EncryptedSharedPreferences is not multi-process coherent. */
        const val ACTIVE_GENIEX_MODEL_FILE = "active_geniex_model.txt"

        private fun String.stripForTts(): String =
            replace(Regex("[*_`#~\\[\\]|>]+"), "")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()

        fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
            val dataBytes = pcm.size * 2
            return ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + dataBytes)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16)
                putShort(1); putShort(1)
                putInt(sampleRate); putInt(sampleRate * 2)
                putShort(2); putShort(16)
                put("data".toByteArray()); putInt(dataBytes)
                for (s in pcm) putShort(s)
            }.array()
        }
    }
}
