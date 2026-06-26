package com.horizons.watchdog.fgs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.horizons.watchdog.kag.KagEmitter
import com.horizons.watchdog.kag.KagEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Runs in :clifford process. Heartbeat is the layer-1 evidence of liveness;
// the KAG event is consumed off-device for cross-layer recovery decisions.
class CliffordService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CLIFFORD")
            .setContentText("watchdog active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        lifecycleScope.launch {
            while (true) {
                Log.d(TAG, "heartbeat")
                KagEmitter.emit(
                    applicationContext,
                    KagEvent(
                        ts = nowIso(),
                        event_type = "heartbeat",
                        device = KagEvent.Device(
                            oem = Build.MANUFACTURER,
                            model = Build.MODEL,
                            chipset = Build.HARDWARE,
                            android_ver = Build.VERSION.SDK_INT
                        ),
                        killer_pkg = null,
                        recovery_layer_hit = 1,
                        latency_ms = 0,
                        outcome = "restored",
                        context = emptyMap()
                    )
                )
                delay(15_000L)
            }
        }

        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "CLIFFORD watchdog",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun nowIso(): String {
        val ms = System.currentTimeMillis()
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(ms))
    }

    companion object {
        private const val TAG = "CliffordService"
        private const val CHANNEL_ID = "clifford_fgs"
        private const val NOTIF_ID = 0xC11FF0
    }
}
