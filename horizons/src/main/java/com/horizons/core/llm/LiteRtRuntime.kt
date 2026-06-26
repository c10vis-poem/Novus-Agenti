package com.horizons.core.llm

// Runtime: Google LiteRT-LM — on-device inference via com.google.ai.edge.litertlm
// Dependency: com.google.ai.edge.litertlm:litertlm-android:0.13.1 (pinned — do not change)
// Model: gemma-4-E2B-it.litertlm  (litert-community/gemma-4-E2B-it-litert-lm, Apache 2.0, ~1.5 GB)
//
// Backend routing on Qualcomm Snapdragon:
//   Backend.GPU()  → Adreno 830 via MLDrift/OpenCL.  ← ACTIVE (E2B GPU build)
//                    gemma-4-E2B-it.litertlm is compiled for mobile GPU.
//   Backend.NPU(nativeLibraryDir) → loads libQnnHtp.so → QAIRT → Hexagon HTP.
//                    Requires NPU-compiled .litertlm (QAT INT4, QNN delegate).
//                    E4B NPU build for SM8750: next fork (Colab recompile via QAI Hub).
//
// Two-phase init:
//   preWarm()   — call from Application.onCreate(). Fires a background IO job that
//                 constructs the Engine and calls initialize(). GPU shader compile runs
//                 while the user is still on the home screen.
//   ensureInit() — called before each inference. Awaits the already-running job; if
//                  preWarm() was skipped it starts the job now.
//
// API surface used:
//   Text  : session.sendMessageAsync(prompt: String)
//   Audio : session.sendMessageAsync(Contents.of(Content.AudioBytes(wav), Content.Text(…)))
//   Vision: session.sendMessageAsync(Contents.of(Content.ImageBytes(jpeg), Content.Text(…)))

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class LiteRtRuntime(
    private val modelPathProvider: () -> String,
    private val nativeLibraryDir: String = "",
    private val cacheDir: String = "",
    private val scope: CoroutineScope,
) : LlmRuntime {

    /** Observable backend status — idle → loading → ready or failed. UI observes this. */
    private val _backendStatus = MutableStateFlow("idle")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    @Volatile private var initJob: Deferred<Engine>? = null

    /**
     * Phase 1 — kick off GPU engine init in the background.
     * Call once from Application.onCreate(). Idempotent.
     */
    override fun preWarm() {
        if (initJob != null) return
        synchronized(this) {
            if (initJob != null) return
            initJob = scope.async(Dispatchers.IO) { buildEngine() }
        }
    }

    private suspend fun buildEngine(): Engine {
        val modelPath = modelPathProvider()
        _backendStatus.value = "loading…"
        Log.i(TAG, "GPU init — model=$modelPath cache=$cacheDir")

        val modelFile = java.io.File(modelPath)
        if (!modelFile.exists()) {
            val msg = "Model not found: $modelPath\nDownload a .litertlm and place it in Downloads."
            _backendStatus.value = "NO MODEL — check Downloads"
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        val sizeMb = modelFile.length() / (1024L * 1024L)
        Log.i(TAG, "Model found: $modelPath (${sizeMb} MB)")

        val cachePath = cacheDir.takeIf { it.isNotBlank() }

        // Attempt 1: normal init with compiled-shader cache.
        try {
            val gpu = Backend.GPU()
            val config = EngineConfig(
                modelPath = modelPath,
                backend = gpu,
                visionBackend = gpu,
                audioBackend = gpu,
                cacheDir = cachePath,
            )
            val e = Engine(config)
            e.initialize()
            _backendStatus.value = "Adreno 830 · GPU · ${sizeMb}MB"
            Log.i(TAG, "GPU engine ready — ${sizeMb}MB at $modelPath")
            return e
        } catch (t: Throwable) {
            Log.w(TAG, "GPU init attempt 1 failed — wiping shader cache and retrying", t)
        }

        // Attempt 2: stale compiled artifacts can cause INTERNAL flatbuffer errors.
        // Wipe every file in cacheDir and retry without it.
        if (cachePath != null) runCatching {
            java.io.File(cachePath).listFiles()?.forEach { it.delete() }
            Log.i(TAG, "Shader cache wiped: $cachePath")
        }
        return try {
            val gpu = Backend.GPU()
            val config = EngineConfig(
                modelPath = modelPath,
                backend = gpu,
                visionBackend = gpu,
                audioBackend = gpu,
                cacheDir = null,
            )
            val e = Engine(config)
            e.initialize()
            _backendStatus.value = "Adreno 830 · GPU · ${sizeMb}MB (no-cache)"
            Log.i(TAG, "GPU engine ready (no-cache) — ${sizeMb}MB at $modelPath")
            e
        } catch (t: Throwable) {
            _backendStatus.value = "GPU FAILED: ${t.message}"
            Log.e(TAG, "GPU engine init failed — model=$modelPath size=${sizeMb}MB", t)
            throw t
        }
    }

    private suspend fun ensureInit(): Engine {
        val job = initJob ?: synchronized(this) {
            initJob ?: scope.async(Dispatchers.IO) { buildEngine() }.also { initJob = it }
        }
        return job.await()
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val engine = ensureInit()
        val session = engine.createConversation()
        session.sendMessageAsync(prompt).collect { emit(it.toString()) }
    }.catch { e ->
        Log.w(TAG, "stream failed", e)
        emit("[LiteRtRuntime error: ${e.message}]")
    }

    override fun streamAudio(wav: ByteArray, prompt: String): Flow<String> = flow {
        val engine = ensureInit()
        val session = engine.createConversation()
        val contents = if (prompt.isNotBlank())
            Contents.of(Content.AudioBytes(wav), Content.Text(prompt))
        else
            Contents.of(Content.AudioBytes(wav))
        session.sendMessageAsync(contents).collect { emit(it.toString()) }
    }.catch { e ->
        Log.w(TAG, "streamAudio failed", e)
        emit("[LiteRtRuntime audio error: ${e.message}]")
    }

    override fun streamImage(jpeg: ByteArray, prompt: String): Flow<String> = flow {
        val engine = ensureInit()
        val session = engine.createConversation()
        val q = prompt.ifBlank { "What is on this screen?" }
        val contents = Contents.of(Content.ImageBytes(jpeg), Content.Text(q))
        session.sendMessageAsync(contents).collect { emit(it.toString()) }
    }.catch { e ->
        Log.w(TAG, "streamImage failed", e)
        emit("[LiteRtRuntime image error: ${e.message}]")
    }

    companion object {
        private const val TAG = "LiteRtRuntime"
    }
}
