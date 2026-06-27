package com.horizons.core.shell

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Proxies Android device resources (audio PCM, screen JPEG, clipboard) to the ort_engine daemon.
 *
 * The C++ daemon has no JVM/Android API access. This class is the bridge:
 * Kotlin app captures the resource, writes it to a temp file, and POSTs the path to the
 * daemon's /api/v1/context/inject endpoint so it can include the resource in its
 * next inference context window.
 *
 * All methods are no-ops if the daemon is not running — callers do not need to guard.
 */
class SecureResourceRelay(private val port: Int = DaemonLauncher.ENGINE_PORT) {

    sealed class Payload {
        data class Audio(val pcm: ByteArray, val sampleRate: Int = 16_000) : Payload()
        data class Image(val jpeg: ByteArray, val width: Int = 0, val height: Int = 0) : Payload()
        data class Clipboard(val text: String) : Payload()
    }

    suspend fun inject(payload: Payload): Boolean = withContext(Dispatchers.IO) {
        try {
            when (payload) {
                is Payload.Audio -> {
                    val tmp = File.createTempFile("relay_audio_", ".raw")
                    tmp.writeBytes(payload.pcm)
                    postInject(JSONObject().apply {
                        put("context_type", "audio")
                        put("sample_rate", payload.sampleRate)
                        put("raw_buffer_path", tmp.absolutePath)
                    })
                }
                is Payload.Image -> {
                    val tmp = File.createTempFile("relay_image_", ".jpg")
                    tmp.writeBytes(payload.jpeg)
                    postInject(JSONObject().apply {
                        put("context_type", "image")
                        put("raw_buffer_path", tmp.absolutePath)
                        if (payload.width > 0)  put("width", payload.width)
                        if (payload.height > 0) put("height", payload.height)
                    })
                }
                is Payload.Clipboard -> {
                    postInject(JSONObject().apply {
                        put("context_type", "clipboard")
                        put("content", payload.text)
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "inject failed", e)
            false
        }
    }

    private fun postInject(body: JSONObject): Boolean {
        val conn = URL("http://127.0.0.1:$port/api/v1/context/inject")
            .openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 2_000
            conn.readTimeout    = 5_000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "/api/v1/context/inject: ${e.message}")
            false
        } finally {
            conn.disconnect()
        }
    }

    companion object { private const val TAG = "SecureResourceRelay" }
}
