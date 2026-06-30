package com.horizons

import android.app.Application
import com.horizons.audio.AudioRecorder
import com.horizons.audio.VadFactory
import com.horizons.audio.VoiceLoopController
import com.horizons.core.llm.LiteRtRuntime
import com.horizons.core.llm.LlmRuntime
import com.horizons.core.llm.NpuClient
import com.horizons.core.log.CrashRecorder
import com.horizons.core.perf.GameModeBoost
import com.horizons.core.perf.GameModeBoost.gameBoosted
import com.horizons.core.agent.AgentLoop
import com.horizons.core.shell.TaskerBridge
import com.horizons.core.state.AppStateStore
import com.horizons.core.voice.KokoroModelManager
import com.horizons.core.voice.KokoroSetupState
import com.horizons.core.voice.SherpaOnnxTtsClient
import com.horizons.provider.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ChatMessage(val role: String, val text: String)

/** Three operating modes for continuous vs. one-shot AI interaction. */
enum class ChatMode {
    /** Mode A — Live Screen Share: continuous MediaProjection + audio loop + Gemma stream (FGS). */
    A,
    /** Mode B — Live Chat: audio loop + Gemma stream, no screen capture (FGS). */
    B,
    /** Mode C — One-shot: manual attach or dock capture; single Gemma call. Default. */
    C,
}

/**
 * Application singleton. All AI inference runs on-device via LiteRtRuntime.
 * Current: Gemma 4 12B, LiteRT-LM, Backend.GPU (Adreno 830).
 * TTS: Sherpa-ONNX → Kokoro multi-lang v1.0 (28 English voices), no Android TTS broker.
 */
class HorizonsApplication : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appState: AppStateStore
        private set

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val tasker: TaskerBridge by lazy { TaskerBridge(this) }

    // ── Agentic loop — LLM + full Android API tool registry ──────────────────
    // Uses a lambda provider so swapping llmRuntime (LiteRt → NpuClient) is transparent.
    val agentLoop: AgentLoop by lazy { AgentLoop(this, { llmRuntime }, tasker, appState) }

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    // ── Kokoro model manager — downloads & extracts the TTS model on first run ─
    val kokoroManager: KokoroModelManager by lazy { KokoroModelManager(this, scope) }

    // ── Sherpa-ONNX TTS (Kokoro voices, no Android TextToSpeech broker) ────────
    val tts: SherpaOnnxTtsClient by lazy { SherpaOnnxTtsClient(kokoroManager.modelDir) }

    // ── Persisted voice settings exposed for RouterPane ──────────────────────────
    val ttsVoiceId: MutableStateFlow<String> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_VOICE) ?: SherpaOnnxTtsClient.DEFAULT_VOICE)
    }
    val ttsSpeed: MutableStateFlow<Float> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_SPEED)?.toFloatOrNull() ?: 1.0f)
    }

    // ── Shared AudioRecorder ──────────────────────────────────────────────────────
    val audioRecorder: AudioRecorder by lazy { AudioRecorder(this) }

    // ── Pending screen capture (dock 👁 button → stored here → chat ask) ──────
    val pendingScreenJpeg = MutableStateFlow<ByteArray?>(null)

    // ── LLM runtime — LiteRT placeholder; auto-swaps to NpuClient once daemon is up ──
    private val _liteRt: LiteRtRuntime by lazy {
        LiteRtRuntime(
            modelPathProvider = { resolveModelPath() },
            nativeLibraryDir  = applicationInfo.nativeLibraryDir,
            cacheDir          = filesDir.absolutePath,
            scope             = scope,
        )
    }
    @Volatile private var _npuClient: NpuClient? = null
    val llmRuntime: LlmRuntime get() = _npuClient ?: _liteRt

    val resolvedModelPath: String get() = resolveModelPath()

    val isNpuActive: Boolean get() = _npuClient != null

    private fun resolveModelPath(): String {
        // /mnt/user/0/emulated/ is a bind-mount visible to Java but not to LiteRT native code.
        // Normalize it to /storage/emulated/ everywhere.
        fun String.fixMntPath() = replace("/mnt/user/0/emulated/", "/storage/emulated/")

        // Stored override — normalize then check readability.
        appState.get(AppStateStore.KEY_LITERT_MODEL_PATH)
            ?.takeIf { it.isNotBlank() }
            ?.fixMntPath()
            ?.also { fixed ->
                // Self-heal stale stored paths so Settings tab shows the canonical form.
                val raw = appState.get(AppStateStore.KEY_LITERT_MODEL_PATH) ?: ""
                if (fixed != raw) appState.put(AppStateStore.KEY_LITERT_MODEL_PATH, fixed)
            }
            ?.let { java.io.File(it) }
            ?.takeIf { it.canRead() }
            ?.let { return it.absolutePath }

        // Shell find — /storage/emulated/0 only; /mnt/user/0/ returns unreliable paths.
        for (findBin in listOf("/system/bin/find", "/system/xbin/find")) {
            if (!java.io.File(findBin).exists()) continue
            try {
                val proc = ProcessBuilder(
                    findBin, "/storage/emulated/0", "-name", "*.litertlm", "-type", "f",
                ).redirectErrorStream(false).start()
                val hit = proc.inputStream.bufferedReader().readLine()?.trim() ?: ""
                proc.destroy()
                if (hit.isNotBlank() && java.io.File(hit).canRead()) return hit
            } catch (_: Exception) { }
            break
        }

        // Java File API fallback — common Download locations + one level of subdirs.
        val roots = listOf(
            java.io.File("/storage/emulated/0/Download"),
            java.io.File("/sdcard/Download"),
            java.io.File("/storage/emulated/0"),
        )
        fun litertlmIn(dir: java.io.File): java.io.File? =
            dir.listFiles { f -> f.isFile && f.name.endsWith(".litertlm") }?.firstOrNull()

        for (root in roots) {
            if (!root.isDirectory) continue
            litertlmIn(root)?.let { return it.absolutePath }
            root.listFiles { f -> f.isDirectory }?.forEach { sub ->
                litertlmIn(sub)?.let { return it.absolutePath }
            }
        }
        return "/storage/emulated/0/Download/gemma-4-E2B-it.litertlm"
    }

    // ── Chat mode (A/B/C) — updated by FGS services on start/stop ────────────
    val chatMode = MutableStateFlow(ChatMode.C)

    // ── Voice loop (Mode B) ────────────────────────────────────────────────────
    val voiceLoop: VoiceLoopController by lazy {
        VoiceLoopController(
            scope = scope,
            recorder = audioRecorder,
            tts = tts,
            engineStreamAudio = { pcm ->
                llmRuntime.streamAudio(pcmToWav(pcm, AudioRecorder.SAMPLE_RATE))
                    .gameBoosted(this@HorizonsApplication)
            },
            vad = VadFactory.create(this@HorizonsApplication),
        )
    }

    // ── Model test (Diagnostics tab) ───────────────────────────────────────────
    val modelTestResult = MutableStateFlow<String?>(null)

    fun testModel() {
        if (_chatBusy.value) return
        modelTestResult.value = "testing…"
        scope.launch {
            var out = ""
            llmRuntime.stream("Say hi in one word.").collect { out += it }
            modelTestResult.value = "backend: ${llmRuntime.backendStatus.value}\n\n" +
                out.take(300).ifBlank { "(empty response)" }
        }
    }

    // ── Chat state ─────────────────────────────────────────────────────────────
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

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
            }
        }
    }

    fun stopAll() {
        tts.stop()
        voiceLoop.stop()
        _chatBusy.value = false
    }

    /** Screen Q&A: JPEG → Gemma 4 vision (LiteRT-LM) → streaming answer in chat. */
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
            }
        }
    }

    /** Voice STT: PCM → WAV → Gemma audio-direct → transcript string. */
    suspend fun transcribeAudio(pcm: ShortArray, sampleRate: Int): String {
        val wav = pcmToWav(pcm, sampleRate)
        var result = ""
        llmRuntime.streamAudio(wav, "Transcribe the audio accurately.").collect { result += it }
        return result.trim()
    }

    override fun onCreate() {
        super.onCreate()
        CrashRecorder(this).install()
        appState = AppStateStore(this)
        _liteRt.preWarm()

        // CLIFFORD (BRD) — launches daemon from FGS context so it inherits
        // oom_score_adj ~-200 to -400. CRS loop monitors + rehydrates. No root, no Shizuku.
        com.horizons.fgs.CliffordService.start(this)

        // Start Kokoro model check/download; init TTS engine once the model is ready.
        kokoroManager.ensureReady()
        scope.launch {
            kokoroManager.state.collect { state ->
                if (state is KokoroSetupState.Ready) {
                    tts.voiceId = ttsVoiceId.value
                    tts.speed   = ttsSpeed.value
                    withContext(Dispatchers.IO) { tts.init() }
                }
            }
        }

        // Keep live voice settings synced to the store.
        scope.launch { ttsVoiceId.collect { id -> tts.voiceId = id; appState.put(AppStateStore.KEY_TTS_VOICE, id) } }
        scope.launch { ttsSpeed.collect  { sp -> tts.speed   = sp; appState.put(AppStateStore.KEY_TTS_SPEED, sp.toString()) } }
    }

    fun activateNpuRuntime() {
        if (_npuClient == null) _npuClient = NpuClient()
    }

    fun resolveNpuModelPath(): String? {
        // Genie .bin (Qwen3-VL-8B, Qwen2.5-VL-7B) checked before legacy .dlc
        val variants = listOf(
            "qwen3_5_9b_language_decoder.bin",
            "qwen3_5_9b_vision_encoder.bin",
            "qwen3_vl_8b_instruct_htp.bin",
            "qwen2_5_vl_7b_instruct_htp.bin",
            "gemma4_12b_qat_htp.dlc",
            "gemma4_E4B_qat_htp.dlc",
            "gemma4_E2B_qat_htp.dlc",
        )
        val roots = listOf(filesDir, java.io.File("/storage/emulated/0/Download"))
        for (root in roots) for (name in variants) {
            val f = java.io.File(root, name)
            if (f.canRead()) return f.absolutePath
        }
        // Shell find — any Genie .bin or QNN .dlc dropped into Downloads
        for (findBin in listOf("/system/bin/find", "/system/xbin/find")) {
            if (!java.io.File(findBin).exists()) continue
            try {
                for (ext in listOf("*.bin", "*.dlc")) {
                    val proc = ProcessBuilder(
                        findBin, "/storage/emulated/0/Download", "-name", ext, "-type", "f",
                    ).redirectErrorStream(false).start()
                    val hit = proc.inputStream.bufferedReader().readLine()?.trim() ?: ""
                    proc.destroy()
                    if (hit.isNotBlank() && java.io.File(hit).canRead()) return hit
                }
            } catch (_: Exception) { }
            break
        }
        return null
    }

    companion object {
        private const val TAG = "HorizonsApp"

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
