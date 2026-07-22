package com.horizons

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Ultra-minimal diagnostic activity — plain Android Views, no Compose, no
 * theme dependency. Reads Breadcrumb boot.log + crash.log and displays them.
 * Separate launcher icon so the user can open it even when the main app crashes.
 */
class CrashDiagnosticActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F172A.toInt())
            setPadding((16 * dp).toInt(), (48 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        val title = TextView(this).apply {
            text = "Novus Agenti — Boot Diagnostics"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Read the text below and share it with your build session."
            textSize = 12f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, (8 * dp).toInt(), 0, (16 * dp).toInt())
        }
        root.addView(subtitle)

        runCatching { com.horizons.core.diag.Breadcrumb.install(this) }

        val logText = try {
            com.horizons.core.diag.Breadcrumb.readAll()
        } catch (e: Throwable) {
            "Failed to read diagnostics: ${e.javaClass.simpleName}: ${e.message}"
        }

        val scroll = ScrollView(this)
        val body = TextView(this).apply {
            text = logText.ifBlank { "(no diagnostic data yet — app hasn't crashed)" }
            textSize = 11f
            setTextColor(0xFF2DD4D9.toInt())
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
        ))

        setContentView(root)
    }
}
