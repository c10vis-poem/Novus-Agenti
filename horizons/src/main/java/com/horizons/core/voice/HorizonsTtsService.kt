package com.horizons.core.voice

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.horizons.HorizonsApplication
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * System TTS provider backed by Sherpa-ONNX Kokoro.
 *
 * When Horizons is set as the device's TTS engine
 * (Settings → Accessibility → Text-to-speech → Preferred engine → Horizons),
 * ANY app calling [TextToSpeech.speak] routes through here instead of
 * Google TTS / Samsung TTS. Fully on-device, no network.
 *
 * Delegates to [SherpaOnnxTtsClient] which runs Kokoro multi-lang v1.0.
 */
class HorizonsTtsService : TextToSpeechService() {

    private val tts get() = (applicationContext as HorizonsApplication).tts

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return if (lang == "eng" || lang == "en") {
            TextToSpeech.LANG_COUNTRY_AVAILABLE
        } else {
            TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun onGetLanguage(): Array<String> = arrayOf("en", "US", "")

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {
        tts.stop()
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString()
            ?: request.text?.toString()
            ?: return

        val engine = tts
        try {
            val kokoroEngine = getKokoroEngine(engine) ?: run {
                Log.w(TAG, "Kokoro engine not initialized — cannot synthesize")
                callback.error()
                return
            }

            val sid = SherpaOnnxTtsClient.ENGLISH_VOICES
                .indexOfFirst { it.id == engine.voiceId }
                .coerceAtLeast(0)
            val generated = kokoroEngine.generate(text = text, sid = sid, speed = engine.speed)

            if (generated.samples.isEmpty()) {
                callback.error()
                return
            }

            val sampleRate = generated.sampleRate
            callback.start(sampleRate, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)

            val pcm = ShortArray(generated.samples.size) { i ->
                (generated.samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }

            val buf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) buf.putShort(s)
            val bytes = buf.array()

            var offset = 0
            val chunkSize = callback.maxBufferSize.coerceAtMost(bytes.size)
            while (offset < bytes.size) {
                val len = (bytes.size - offset).coerceAtMost(chunkSize)
                callback.audioAvailable(bytes, offset, len)
                offset += len
            }
            callback.done()
        } catch (e: Exception) {
            Log.e(TAG, "synthesize failed", e)
            callback.error()
        }
    }

    private fun getKokoroEngine(client: SherpaOnnxTtsClient): com.k2fsa.sherpa.onnx.OfflineTts? {
        return try {
            val field = SherpaOnnxTtsClient::class.java.getDeclaredField("engine")
            field.isAccessible = true
            field.get(client) as? com.k2fsa.sherpa.onnx.OfflineTts
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "HorizonsTts"
    }
}
