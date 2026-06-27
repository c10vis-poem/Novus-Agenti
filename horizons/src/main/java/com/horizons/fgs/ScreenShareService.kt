package com.horizons.fgs

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.horizons.ChatMode
import com.horizons.HorizonsApplication
import com.horizons.audio.AudioRecorder
import com.horizons.audio.VadFactory
import com.horizons.audio.VoiceLoopController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Mode A — Live Screen Share FGS.
 *
 * foregroundServiceType: mediaProjection|microphone
 * Captures the screen every 5 seconds and sends each JPEG to screenAsk().
 * Also runs the voice loop (audio barge-in).
 * Sticky notification: tap to stop and return to Mode C.
 *
 * The caller MUST pass the MediaProjection consent result via Intent extras:
 *   EXTRA_RESULT_CODE → Activity result code
 *   EXTRA_RESULT_DATA → The consent Intent returned by MediaProjectionManager
 */
class ScreenShareService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var captureJob: Job? = null
    private var voiceLoop: VoiceLoopController? = null

    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData: Intent? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            Log.w(TAG, "No valid MediaProjection consent — stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val app = applicationContext as HorizonsApplication

        // Acquire MediaProjection AFTER FGS is started (API 34+ requirement)
        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            Log.e(TAG, "getMediaProjection returned null")
            stopSelf()
            return START_NOT_STICKY
        }
        mediaProjection = projection

        handlerThread = HandlerThread("ScreenShareCapture").apply { start() }
        handler = Handler(handlerThread.looper)

        // Register callback so we know when projection is stopped externally
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection stopped externally")
                stopSelf()
            }
        }, handler)

        // Start continuous screen capture loop (every 5 s)
        captureJob = app.scope.launch {
            while (isActive) {
                try {
                    val jpeg = captureJpeg(projection) ?: continue
                    app.screenAsk(jpeg, "")
                } catch (e: Throwable) {
                    Log.w(TAG, "captureJpeg error", e)
                }
                delay(5_000L)
            }
        }

        // Start voice loop for barge-in
        val loop = VoiceLoopController(
            scope = app.scope,
            recorder = AudioRecorder(this),
            tts = app.tts,
            engineStreamAudio = { pcm ->
                app.llmRuntime.streamAudio(HorizonsApplication.pcmToWav(pcm, AudioRecorder.SAMPLE_RATE))
            },
            vad = VadFactory.create(this),
            continuousMode = true,
        )
        voiceLoop = loop
        loop.startLoop()

        app.chatMode.value = ChatMode.A
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        captureJob?.cancel()
        captureJob = null
        voiceLoop?.stop()
        voiceLoop = null
        mediaProjection?.stop()
        mediaProjection = null
        if (::handlerThread.isInitialized) handlerThread.quitSafely()
        val app = applicationContext as HorizonsApplication
        if (app.chatMode.value == ChatMode.A) {
            app.chatMode.value = ChatMode.C
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Screen capture ────────────────────────────────────────────────────────

    private suspend fun captureJpeg(projection: MediaProjection): ByteArray? {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val bounds = wm.currentWindowMetrics.bounds
        val width = bounds.width()
        val height = bounds.height()
        val density = resources.displayMetrics.densityDpi
            .takeIf { it > 0 } ?: android.util.DisplayMetrics.DENSITY_DEFAULT

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay = projection.createVirtualDisplay(
            "horizons-live-share",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler,
        ) ?: return null

        return try {
            val bitmap = withTimeoutOrNull(3_000L) {
                awaitFrame(reader, width, height)
            }
            if (bitmap == null) {
                Log.w(TAG, "Timed out waiting for screen frame")
                return null
            }
            val scaled = downscale(bitmap, 1024)
            bitmap.recycle()
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            out.toByteArray()
        } finally {
            runCatching { virtualDisplay.release() }
            runCatching { reader.close() }
        }
    }

    private suspend fun awaitFrame(reader: ImageReader, width: Int, height: Int): Bitmap =
        suspendCancellableCoroutine { cont ->
            val listener = object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(r: ImageReader) {
                    val image = try { r.acquireLatestImage() } catch (t: Throwable) { null }
                        ?: return
                    try {
                        val plane = image.planes[0]
                        val rowPadding = plane.rowStride - plane.pixelStride * width
                        val bmpWidth = width + rowPadding / plane.pixelStride
                        val bmp = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
                        bmp.copyPixelsFromBuffer(plane.buffer)
                        val result = if (bmpWidth != width) {
                            Bitmap.createBitmap(bmp, 0, 0, width, height).also { bmp.recycle() }
                        } else bmp
                        r.setOnImageAvailableListener(null, null)
                        if (cont.isActive) cont.resume(result)
                    } catch (t: Throwable) {
                        if (cont.isActive) cont.cancel(t)
                    } finally {
                        runCatching { image.close() }
                    }
                }
            }
            reader.setOnImageAvailableListener(listener, handler)
            cont.invokeOnCancellation {
                runCatching { reader.setOnImageAvailableListener(null, null) }
            }
        }

    private fun downscale(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val scale = maxDim.toFloat() / longest
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Live Screen Share",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Horizons is sharing your screen" }
            )
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ScreenShareService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle("Live Screen Share active")
            .setContentText("Tap to stop")
            .setOngoing(true)
            .setContentIntent(stopPi)
            .addAction(
                Notification.Action.Builder(null, "Stop", stopPi).build()
            )
            .build()
    }

    companion object {
        private const val TAG = "ScreenShareService"
        const val CHANNEL_ID = "screen_share"
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "com.horizons.fgs.STOP_SCREEN_SHARE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
    }
}
