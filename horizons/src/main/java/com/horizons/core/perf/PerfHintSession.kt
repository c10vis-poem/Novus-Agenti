package com.horizons.core.perf

import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager
import android.os.Process
import android.util.Log

/**
 * Per-frame CPU/GPU boost via [PerformanceHintManager] (API 31+). Pairs with
 * the GAME power mode from [GameModeBoost]; ADPF only schedules sustained clocks
 * if a real consumer reports work durations.
 *
 * Usage:
 *   val s = PerfHintSession.create(context, targetMs = 50)
 *   val start = System.nanoTime()
 *   doInferenceBurst()
 *   s?.report(System.nanoTime() - start)
 *   s?.close()
 */
class PerfHintSession private constructor(
    private val session: PerformanceHintManager.Session
) {
    fun report(durationNanos: Long) {
        runCatching { session.reportActualWorkDuration(durationNanos) }
            .onFailure { Log.w(TAG, "report failed", it) }
    }

    fun close() {
        runCatching { session.close() }.onFailure { Log.w(TAG, "close failed", it) }
    }

    companion object {
        private const val TAG = "PerfHintSession"

        fun create(context: Context, targetMs: Long = 50L): PerfHintSession? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            return runCatching {
                val phm = context.getSystemService(PerformanceHintManager::class.java)
                    ?: return null
                val tids = intArrayOf(Process.myTid())
                val session = phm.createHintSession(tids, targetMs * 1_000_000L)
                    ?: return null
                PerfHintSession(session)
            }.getOrElse {
                Log.w(TAG, "create failed", it)
                null
            }
        }
    }
}
