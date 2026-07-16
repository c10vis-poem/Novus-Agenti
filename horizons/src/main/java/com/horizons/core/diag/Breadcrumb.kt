package com.horizons.core.diag

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Append-only startup diagnostic log. Writes to the app's external files dir
 * so the file is visible in any file manager under
 *   /sdcard/Android/data/com.horizons/files/diag/boot.log
 * No permission required (getExternalFilesDir is per-app scoped storage).
 *
 * Every major lifecycle step calls [drop] with a short tag. If the app dies
 * mid-startup, the last entry in boot.log shows exactly where.
 *
 * Also installs an UncaughtExceptionHandler that writes the full stack trace
 * to crash.log right before the process dies.
 */
object Breadcrumb {

    private const val BOOT_FILE  = "boot.log"
    private const val CRASH_FILE = "crash.log"
    private const val MAX_BOOT_SIZE_BYTES = 256L * 1024L  // 256 KiB cap

    // Mirror copies in the PUBLIC Downloads folder. Android/data is walled off
    // from file managers and Termux on modern Android (session-17 lesson: the
    // operator could not open boot.log at all) — Downloads is the one place on
    // the phone everything can read. Writes need the All-files-access grant
    // (MANAGE_EXTERNAL_STORAGE, exposed in SettingsPane); without it they fail
    // silently and the private copies still work.
    private const val PUBLIC_BOOT  = "/storage/emulated/0/Download/novus-boot.log"
    private const val PUBLIC_CRASH = "/storage/emulated/0/Download/novus-crash.log"

    @Volatile private var dir: File? = null
    @Volatile private var lastCrumb: String = "init"

    fun install(ctx: Context) {
        val d = ctx.getExternalFilesDir(null)?.let { File(it, "diag") }
            ?: File(ctx.filesDir, "diag")
        d.mkdirs()
        dir = d

        rotateIfTooBig(File(d, BOOT_FILE))
        rotateIfTooBig(File(PUBLIC_BOOT))

        drop("session_start " +
            "pid=${Process.myPid()} " +
            "android=${Build.VERSION.SDK_INT} " +
            "device=${Build.MANUFACTURER}/${Build.MODEL}")

        val upstream = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val entry = "${ts()} thread=${thread.name} last_crumb=$lastCrumb\n" +
                throwable.stackTraceToString() +
                "\n----\n"
            runCatching { File(d, CRASH_FILE).appendText(entry) }
            runCatching { File(PUBLIC_CRASH).appendText(entry) }
            upstream?.uncaughtException(thread, throwable)
        }
    }

    /** Append a single breadcrumb. Cheap — no allocation beyond the line itself. */
    fun drop(tag: String) {
        lastCrumb = tag
        val d = dir ?: return
        val line = "${ts()} $tag\n"
        runCatching {
            FileWriter(File(d, BOOT_FILE), true).use { w -> w.write(line) }
        }
        runCatching {
            FileWriter(File(PUBLIC_BOOT), true).use { w -> w.write(line) }
        }
    }

    /** Read entire boot.log + crash.log for in-app display. */
    fun readAll(): String {
        val d = dir ?: return "(diag dir not initialized)"
        val boot = File(d, BOOT_FILE).takeIf { it.canRead() }?.readText() ?: "(no boot.log)"
        val crash = File(d, CRASH_FILE).takeIf { it.canRead() }?.readText() ?: ""
        return buildString {
            append("== boot.log ==\n").append(boot)
            if (crash.isNotEmpty()) append("\n== crash.log ==\n").append(crash)
        }
    }

    /** Most recent breadcrumb across ALL processes — reads boot.log tail.
     *  Use as FGS notification text so the user can see what the main process
     *  was doing right before it died, without opening the app. */
    fun last(): String {
        val d = dir ?: return lastCrumb
        val f = File(d, BOOT_FILE)
        if (!f.canRead()) return lastCrumb
        return runCatching {
            f.useLines { lines -> lines.lastOrNull() }
                ?.substringAfter(' ', missingDelimiterValue = lastCrumb)
                ?: lastCrumb
        }.getOrDefault(lastCrumb)
    }

    fun clear() {
        val d = dir ?: return
        File(d, BOOT_FILE).delete()
        File(d, CRASH_FILE).delete()
        lastCrumb = "cleared"
    }

    private fun ts(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    private fun rotateIfTooBig(f: File) {
        if (f.exists() && f.length() > MAX_BOOT_SIZE_BYTES) {
            f.renameTo(File(f.parentFile, "${f.name}.prev"))
        }
    }
}
