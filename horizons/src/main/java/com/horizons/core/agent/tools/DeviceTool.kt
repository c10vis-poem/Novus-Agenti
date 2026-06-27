package com.horizons.core.agent.tools

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.horizons.core.agent.ToolResult
import org.json.JSONObject

class DeviceTool(private val context: Context) {

    // ── WiFi ──────────────────────────────────────────────────────────────────

    fun wifi(action: String): ToolResult {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ToolResult("wifi", false, "WifiManager unavailable")
        return when (action.lowercase()) {
            "status" -> ToolResult("wifi", true, """{"enabled":${wm.isWifiEnabled}}""")
            "on", "off" -> {
                val enable = action == "on"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Direct toggle removed in API 29 — open panel instead
                    val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(panelIntent)
                    ToolResult("wifi", true, "Opened Wi-Fi settings panel (Android 10+ requires user confirmation)")
                } else {
                    @Suppress("DEPRECATION")
                    wm.isWifiEnabled = enable
                    ToolResult("wifi", true, "WiFi turned $action")
                }
            }
            else -> ToolResult("wifi", false, "Unknown action: $action. Use on|off|status")
        }
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────

    fun bluetooth(action: String): ToolResult {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return ToolResult("bluetooth", false, "BluetoothManager unavailable")
        val adapter = bm.adapter ?: return ToolResult("bluetooth", false, "No Bluetooth adapter")
        return when (action.lowercase()) {
            "status" -> ToolResult("bluetooth", true, """{"enabled":${adapter.isEnabled},"state":${adapter.state}}""")
            "on"  -> {
                if (adapter.isEnabled) return ToolResult("bluetooth", true, "Bluetooth already on")
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult("bluetooth", true, "Bluetooth enable dialog shown")
            }
            "off" -> {
                if (!adapter.isEnabled) return ToolResult("bluetooth", true, "Bluetooth already off")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult("bluetooth", true, "Opened Bluetooth settings (Android 13+ requires user confirmation)")
                } else {
                    @Suppress("DEPRECATION")
                    adapter.disable()
                    ToolResult("bluetooth", true, "Bluetooth disabled")
                }
            }
            else -> ToolResult("bluetooth", false, "Unknown action: $action. Use on|off|status")
        }
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    fun volume(stream: String, level: Int): ToolResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamType = STREAM_MAP[stream.lowercase()]
            ?: return ToolResult("volume", false, "Unknown stream: $stream. Use music|ring|alarm|notification")
        return if (level == -1) {
            val current = am.getStreamVolume(streamType)
            val max     = am.getStreamMaxVolume(streamType)
            ToolResult("volume", true, """{"stream":"$stream","current":$current,"max":$max}""")
        } else {
            val clamped = level.coerceIn(0, am.getStreamMaxVolume(streamType))
            am.setStreamVolume(streamType, clamped, AudioManager.FLAG_SHOW_UI)
            ToolResult("volume", true, "Volume set: $stream → $clamped")
        }
    }

    // ── Brightness ────────────────────────────────────────────────────────────

    fun brightness(level: Int): ToolResult {
        return if (level == -1) {
            val current = try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (_: Settings.SettingNotFoundException) { -1 }
            ToolResult("brightness", true, """{"current":$current,"max":255}""")
        } else {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult("brightness", false, "WRITE_SETTINGS not granted — opened permission screen")
            }
            val clamped = level.coerceIn(0, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, clamped)
            ToolResult("brightness", true, "Brightness set to $clamped")
        }
    }

    // ── Do Not Disturb ────────────────────────────────────────────────────────

    fun dnd(mode: String): ToolResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult("dnd", false, "DND policy access not granted — opened settings")
        }
        return when (mode.lowercase()) {
            "status" -> {
                val filter = nm.currentInterruptionFilter
                val on = filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                ToolResult("dnd", true, """{"on":$on,"filter":$filter}""")
            }
            "on" -> {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
                ToolResult("dnd", true, "Do Not Disturb enabled")
            }
            "off" -> {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                ToolResult("dnd", true, "Do Not Disturb disabled")
            }
            else -> ToolResult("dnd", false, "Unknown mode: $mode. Use on|off|status")
        }
    }

    // ── Flashlight ────────────────────────────────────────────────────────────

    fun flashlight(action: String): ToolResult {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ToolResult("flashlight", false, "CameraManager unavailable")
        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult("flashlight", false, "No flash on this device")
        return try {
            val on = action.lowercase() == "on"
            cm.setTorchMode(cameraId, on)
            ToolResult("flashlight", true, "Flashlight ${if (on) "on" else "off"}")
        } catch (e: Exception) {
            ToolResult("flashlight", false, e.message ?: "torch toggle failed")
        }
    }

    private companion object {
        val STREAM_MAP = mapOf(
            "music"        to AudioManager.STREAM_MUSIC,
            "ring"         to AudioManager.STREAM_RING,
            "alarm"        to AudioManager.STREAM_ALARM,
            "notification" to AudioManager.STREAM_NOTIFICATION,
        )
    }
}
