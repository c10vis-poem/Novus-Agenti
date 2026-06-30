package com.horizons.fgs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.horizons.ChatMode
import com.horizons.HorizonsApplication
import com.horizons.audio.AudioRecorder
import com.horizons.audio.VadFactory
import com.horizons.audio.VoiceLoopController

/**
 * Mode B — Live Chat FGS.
 *
 * foregroundServiceType: microphone
 * Runs the audio loop + LLM stream continuously. No screen capture.
 * Sticky notification: tap to stop the service and return to Mode C.
 */
class LiveChatService : Service() {

    private var voiceLoop: VoiceLoopController? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val app = applicationContext as HorizonsApplication
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

        app.chatMode.value = ChatMode.B
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceLoop?.stop()
        voiceLoop = null
        val app = applicationContext as HorizonsApplication
        if (app.chatMode.value == ChatMode.B) {
            app.chatMode.value = ChatMode.C
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Live Chat",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Horizons live chat is active" }
            )
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, LiveChatService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Live Chat active")
            .setContentText("Tap to stop")
            .setOngoing(true)
            .setContentIntent(stopPi)
            .addAction(
                Notification.Action.Builder(null, "Stop", stopPi).build()
            )
            .build()
    }

    companion object {
        const val CHANNEL_ID = "live_chat"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.horizons.fgs.STOP_LIVE_CHAT"
    }
}
