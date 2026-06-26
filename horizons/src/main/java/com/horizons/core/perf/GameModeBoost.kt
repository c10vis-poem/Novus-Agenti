package com.horizons.core.perf

import android.app.GameManager
import android.app.GameState
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Tells the Android Dynamic Performance Framework (ADPF) that we are in a
 * non-interruptible hot loop. Combined with `appCategory="game"` + `isGame="true"`
 * in the manifest, this triggers the Power HAL's GAME power mode on Android 14+,
 * which on Snapdragon hardware sustains GPU/NPU clocks instead of downclocking.
 *
 * Wrap any LLM inference burst (vision-on-NPU, reasoning-on-GPU, audio in/out)
 * with [enterHotLoop] / [exitHotLoop]. Safe to call from any thread.
 */
object GameModeBoost {
    private const val TAG = "GameModeBoost"

    // ADPF per-thread CPU/GPU scheduler hint, held for the duration of a hot loop.
    // Keeps the inference orchestration thread pinned to big cores so it feeds the
    // Hexagon NPU without scheduler-induced stalls.
    @Volatile private var hint: PerfHintSession? = null

    // Reentrancy depth: nested wrappers (e.g. sendChat wrapping a stream that
    // AgentLoop also wraps) increment instead of leaking a second PerfHintSession.
    // Only the outermost enter/exit actually engages/disengages the GAME mode.
    private val depth = java.util.concurrent.atomic.AtomicInteger(0)

    fun enterHotLoop(context: Context) {
        if (depth.getAndIncrement() != 0) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val gm = context.getSystemService(Context.GAME_SERVICE) as? GameManager
                ?: return
            gm.setGameState(GameState(false, GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE))
        }.onFailure { Log.w(TAG, "enterHotLoop failed", it) }
        hint = PerfHintSession.create(context, targetMs = 16L)
    }

    /** Report a completed work burst (e.g. one streamed chunk) so ADPF can raise clocks. */
    fun reportWork(durationNanos: Long) {
        hint?.report(durationNanos)
    }

    fun exitHotLoop(context: Context) {
        if (depth.decrementAndGet() != 0) return
        hint?.close()
        hint = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val gm = context.getSystemService(Context.GAME_SERVICE) as? GameManager
                ?: return
            gm.setGameState(GameState(false, GameState.MODE_NONE))
        }.onFailure { Log.w(TAG, "exitHotLoop failed", it) }
    }

    /**
     * Flow operator that wraps every LlmRuntime stream call with Game Mode boost —
     * enter on subscription, report per chunk, exit on completion/cancel.
     * Use at every site that calls llmRuntime.stream/streamAudio/streamImage.
     */
    fun <T> kotlinx.coroutines.flow.Flow<T>.gameBoosted(
        context: Context,
    ): kotlinx.coroutines.flow.Flow<T> {
        var lastNanos = 0L
        return kotlinx.coroutines.flow.flow {
            try {
                enterHotLoop(context)
                lastNanos = System.nanoTime()
                collect { v ->
                    val now = System.nanoTime()
                    reportWork(now - lastNanos)
                    lastNanos = now
                    emit(v)
                }
            } finally {
                exitHotLoop(context)
            }
        }
    }
}
