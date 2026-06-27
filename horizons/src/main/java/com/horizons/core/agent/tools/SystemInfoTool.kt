package com.horizons.core.agent.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.horizons.core.agent.ToolResult
import org.json.JSONObject

class SystemInfoTool(private val context: Context) {

    fun battery(): ToolResult {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
            ?: return ToolResult("battery", false, "Battery intent unavailable")
        val level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val temp    = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val pct     = if (scale > 0) level * 100 / scale else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL
        val obj = JSONObject().apply {
            put("percent",      pct)
            put("charging",     charging)
            put("temperature_c", temp / 10.0)
            put("status",       statusLabel(status))
        }
        return ToolResult("battery", true, obj.toString())
    }

    fun network(): ToolResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps    = cm.getNetworkCapabilities(network)
        val obj     = JSONObject()
        if (caps == null) {
            obj.put("connected", false)
        } else {
            obj.put("connected", true)
            obj.put("wifi",    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            obj.put("cell",    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            obj.put("vpn",     caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            obj.put("internet", caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            // SSID (requires location or nearby-wifi permission; best-effort)
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ssid = runCatching { wm?.connectionInfo?.ssid?.replace("\"", "") }.getOrNull() ?: ""
            if (ssid.isNotBlank() && ssid != "<unknown ssid>") obj.put("ssid", ssid)
        }
        return ToolResult("network", true, obj.toString())
    }

    fun storage(): ToolResult {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free  = stat.availableBytes
        val obj   = JSONObject().apply {
            put("total_gb", "%.2f".format(total / 1e9))
            put("free_gb",  "%.2f".format(free  / 1e9))
            put("used_gb",  "%.2f".format((total - free) / 1e9))
            put("free_pct", if (total > 0) (free * 100 / total) else 0)
        }
        return ToolResult("storage", true, obj.toString())
    }

    private fun statusLabel(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING    -> "charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
        BatteryManager.BATTERY_STATUS_FULL        -> "full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
        else -> "unknown"
    }
}
