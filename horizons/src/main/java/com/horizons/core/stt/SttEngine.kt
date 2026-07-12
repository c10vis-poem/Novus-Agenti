package com.horizons.core.stt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * System-level speech-to-text — the app's OWN transcription layer, independent
 * of whichever LLM backend is loaded.
 *
 * This is deliberately NOT the LLM. Voice input flows:
 *   mic → Silero VAD (endpoint) → SttEngine (transcribe) → LLM (reason) → TTS
 *
 * A plugged-in model never provides STT unless the user explicitly opts into a
 * model's native audio path; by default transcription is always ours.
 */
interface SttEngine {

    /** True once the model is loaded and [transcribe] can run. */
    val ready: StateFlow<Boolean>

    /** Human-readable engine identity for the Settings / voice UI. */
    val status: StateFlow<String>

    /** Load the model. Safe to call more than once; a no-op once ready. */
    fun init()

    /**
     * Transcribe 16-bit PCM mono audio to text. Returns "" on empty/failed
     * input or when the engine isn't ready. Never throws to the caller.
     */
    suspend fun transcribe(pcm: ShortArray, sampleRate: Int): String

    /** Release native resources. */
    fun close()

    companion object {
        /** A no-op engine used before a real one is wired / while a model downloads. */
        val NONE: SttEngine = object : SttEngine {
            override val ready = MutableStateFlow(false)
            override val status = MutableStateFlow("STT · no engine")
            override fun init() {}
            override suspend fun transcribe(pcm: ShortArray, sampleRate: Int) = ""
            override fun close() {}
        }
    }
}
