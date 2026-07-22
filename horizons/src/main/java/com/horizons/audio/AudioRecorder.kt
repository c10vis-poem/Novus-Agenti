package com.horizons.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Push-to-talk PCM16 capture (mono, 16 kHz).
 *
 * Records via [AudioRecord] using [MediaRecorder.AudioSource.VOICE_RECOGNITION],
 * accumulates chunks into a [ShortArray], and returns it from [stop].
 * Output is passed to :1234/v1/audio/transcriptions (Whisper-compatible) as a WAV file.
 */
class AudioRecorder(private val context: Context) {

    private val running = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var readerJob: Job? = null

    private val accumulator = ArrayList<ShortArray>()
    private var accumulatedSize: Int = 0
    private var readBufferSize: Int = 0

    /**
     * Live PCM chunks, emitted as they are read from the mic while recording.
     * Lets VAD (endpointing + barge-in) observe audio WITHOUT stop/restarting
     * the recorder. Chunks are also accumulated for [stop] as before.
     */
    private val _chunks = MutableSharedFlow<ShortArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val chunks: SharedFlow<ShortArray> = _chunks

    fun isRecording(): Boolean = running.get()

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (running.get()) {
            return@withContext Result.failure(IllegalStateException("AudioRecorder already started"))
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
        }
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuf <= 0) {
            return@withContext Result.failure(
                IllegalStateException("AudioRecord.getMinBufferSize returned $minBuf")
            )
        }
        val bufferSizeBytes = minBuf * 2
        @Suppress("MissingPermission")
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSizeBytes,
            )
        } catch (t: Throwable) {
            return@withContext Result.failure(
                IllegalStateException("Failed to construct AudioRecord: ${t.message}", t)
            )
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { rec.release() }
            return@withContext Result.failure(
                IllegalStateException("AudioRecord not initialized (state=${rec.state})")
            )
        }
        accumulator.clear(); accumulatedSize = 0
        readBufferSize = bufferSizeBytes / 2
        try { rec.startRecording() } catch (t: Throwable) {
            runCatching { rec.release() }
            return@withContext Result.failure(
                IllegalStateException("AudioRecord.startRecording failed: ${t.message}", t)
            )
        }
        recorder = rec; running.set(true)
        readerJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val buf = ShortArray(readBufferSize)
            try {
                while (running.get()) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) {
                        val chunk = buf.copyOf(n)
                        accumulator.add(chunk); accumulatedSize += n
                        _chunks.tryEmit(chunk)
                    }
                    else if (n < 0) { Log.w(TAG, "AudioRecord.read error $n"); running.set(false); break }
                }
            } catch (t: Throwable) { Log.e(TAG, "Reader loop crashed", t); running.set(false) }
        }
        Result.success(Unit)
    }

    suspend fun stop(): Result<ShortArray> {
        if (!running.get() && recorder == null) {
            return Result.failure(IllegalStateException("AudioRecorder not started"))
        }
        running.set(false)
        readerJob?.join(); readerJob = null
        val rec = recorder; recorder = null
        return withContext(Dispatchers.IO) {
            try {
                if (rec != null) {
                    runCatching {
                        if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop()
                    }
                    runCatching { rec.release() }
                }
                val out = ShortArray(accumulatedSize)
                var offset = 0
                for (chunk in accumulator) {
                    System.arraycopy(chunk, 0, out, offset, chunk.size)
                    offset += chunk.size
                }
                accumulator.clear(); accumulatedSize = 0
                Result.success(out)
            } catch (t: Throwable) {
                runCatching { rec?.release() }
                accumulator.clear(); accumulatedSize = 0
                Result.failure(t)
            }
        }
    }

    companion object {
        const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
