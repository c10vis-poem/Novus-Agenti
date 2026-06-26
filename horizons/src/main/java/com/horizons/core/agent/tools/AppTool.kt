package com.horizons.core.agent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.horizons.core.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject

class AppTool(private val context: Context) {

    fun launch(appQuery: String): ToolResult {
        val pm = context.packageManager
        val candidates = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.GET_META_DATA,
        )
        // Match by label (case-insensitive) or package name
        val q = appQuery.lowercase()
        val match = candidates.firstOrNull { ri ->
            ri.loadLabel(pm).toString().lowercase().contains(q) ||
                ri.activityInfo.packageName.lowercase().contains(q)
        } ?: return ToolResult("launch_app", false, "No app found matching \"$appQuery\"")

        return try {
            val intent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
                ?: return ToolResult("launch_app", false, "No launch intent for ${match.activityInfo.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult("launch_app", true, "Launched ${match.loadLabel(pm)}")
        } catch (e: Exception) {
            ToolResult("launch_app", false, e.message ?: "launch failed")
        }
    }

    fun list(): ToolResult {
        val pm = context.packageManager
        val candidates = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.GET_META_DATA,
        )
        val arr = JSONArray()
        candidates.sortedBy { it.loadLabel(pm).toString() }.forEach { ri ->
            arr.put(JSONObject().apply {
                put("label",   ri.loadLabel(pm).toString())
                put("package", ri.activityInfo.packageName)
            })
        }
        return ToolResult("list_apps", true, arr.toString())
    }
}
