package com.horizons.core.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LlmRuntime {
    fun stream(prompt: String): Flow<String>

    fun streamAudio(wav: ByteArray, prompt: String = ""): Flow<String> =
        stream(if (prompt.isNotBlank()) prompt else "[audio input not supported by this runtime]")

    fun streamImage(jpeg: ByteArray, prompt: String = ""): Flow<String> =
        stream(if (prompt.isNotBlank()) "[image] $prompt" else "[image input not supported by this runtime]")

    val backendStatus: StateFlow<String>
        get() = idleStatus

    val perfMetrics: StateFlow<PerfMetrics?>
        get() = noMetrics

    val thinkingActive: StateFlow<Boolean>
        get() = notThinking

    val capabilities: Capabilities
        get() = Capabilities.NONE

    fun preWarm() {}

    data class PerfMetrics(
        val firstTokenMs: Long,
        val tokensPerSec: Double,
        val tokenCount: Int,
    )

    data class Capabilities(
        val supportsVision: Boolean = false,
        val supportsThinking: Boolean = false,
        val supportsToolCalling: Boolean = false,
        val maxContextLength: Int = 4096,
        val runtimeFamily: String = "unknown",
    ) {
        companion object {
            val NONE = Capabilities()
        }
    }

    companion object {
        private val idleStatus: StateFlow<String> = MutableStateFlow("idle")
        private val noMetrics: StateFlow<PerfMetrics?> = MutableStateFlow(null)
        private val notThinking: StateFlow<Boolean> = MutableStateFlow(false)
    }
}
