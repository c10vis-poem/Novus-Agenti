package com.horizons.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.horizons.HorizonsApplication
import com.horizons.audio.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating side dock — visible on every screen while the accessibility service is enabled.
 *
 *  🎙 Mic    — tap to record, tap again to stop; transcribes via Qwen3.5-9B.
 *              If a screenshot is pending, sends it as a vision Q&A; otherwise pastes
 *              the transcript into the focused text field or opens Chat.
 *  👁 Vision — captures the current screen → stores JPEG in app.pendingScreenJpeg.
 *  ⏹ Stop   — cancels in-flight inference and stops TTS mid-sentence.
 *
 * Gesture controls:
 *  • Drag       — move dock anywhere on screen; a red ✕ appears at the bottom.
 *  • Drop on ✕  — removes the dock for this session.
 *  • Pinch      — resize the dock (0.5× – 2.0×).
 *  • Double-tap — cycle transparency: 85% → 55% → 30% → 10% → 85%.
 */
class HorizonsAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var dockView: LinearLayout? = null
    private var dismissTarget: TextView? = null
    private lateinit var dockParams: WindowManager.LayoutParams

    private var focusedEditNode: AccessibilityNodeInfo? = null
    @Volatile private var isRecording = false

    // Drag state
    private var dragStartParamX = 0
    private var dragStartParamY = 0
    private var touchStartRawX = 0f
    private var touchStartRawY = 0f
    private var isDragging = false
    private var lastTapMs = 0L

    // Appearance
    private var dockAlpha = 0.85f

    private val app get() = applicationContext as HorizonsApplication

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        buildDock()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val node = event.source ?: return
            if (node.isEditable) {
                focusedEditNode?.recycle()
                focusedEditNode = AccessibilityNodeInfo.obtain(node)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        job.cancel()
        removeDock()
        focusedEditNode?.recycle()
        super.onDestroy()
    }

    // ── Dock construction ──────────────────────────────────────────────────────

    private fun buildDock() {
        val wm = windowManager ?: return
        val dp = resources.displayMetrics.density
        val screenH = resources.displayMetrics.heightPixels

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC0F172A.toInt())
            alpha = dockAlpha
            setPadding(
                (8 * dp).toInt(), (16 * dp).toInt(),
                (8 * dp).toInt(), (16 * dp).toInt(),
            )
        }

        val micBtn  = dockBtn("🎙", dp) { handleMic() }
        val eyeBtn  = dockBtn("👁", dp) { handleVision() }
        val stopBtn = dockBtn("⏹", dp) { handleStop() }
        layout.addView(micBtn)
        layout.addView(eyeBtn)
        layout.addView(stopBtn)

        // Pinch-to-scale
        val scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    val s = (layout.scaleX * d.scaleFactor).coerceIn(0.5f, 2.0f)
                    layout.scaleX = s
                    layout.scaleY = s
                    return true
                }
            })

        layout.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            if (scaleDetector.isInProgress) return@setOnTouchListener true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartParamX = dockParams.x
                    dragStartParamY = dockParams.y
                    touchStartRawX  = event.rawX
                    touchStartRawY  = event.rawY
                    isDragging = false
                    // Double-tap → cycle transparency
                    val now = System.currentTimeMillis()
                    if (now - lastTapMs < 300L) cycleAlpha(layout)
                    lastTapMs = now
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartRawX
                    val dy = event.rawY - touchStartRawY
                    if (!isDragging && (abs(dx) > 12 || abs(dy) > 12)) {
                        isDragging = true
                        showDismissTarget()
                    }
                    if (isDragging) {
                        dockParams.x = (dragStartParamX + dx).roundToInt()
                        dockParams.y = (dragStartParamY + dy).roundToInt()
                        runCatching { wm.updateViewLayout(layout, dockParams) }
                        val overX = event.rawY > resources.displayMetrics.heightPixels * 0.82f
                        dismissTarget?.alpha = if (overX) 1.0f else 0.6f
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasDragging = isDragging
                    isDragging = false
                    if (wasDragging) {
                        val overX = event.rawY > resources.displayMetrics.heightPixels * 0.82f
                        hideDismissTarget()
                        if (overX) { removeDock(); return@setOnTouchListener true }
                    } else {
                        // Single tap — hit-test children and dispatch click
                        for (i in 0 until layout.childCount) {
                            val child = layout.getChildAt(i)
                            if (event.x >= child.left && event.x <= child.right &&
                                event.y >= child.top  && event.y <= child.bottom) {
                                child.performClick(); break
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }

        dockParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = screenH / 3
        }

        wm.addView(layout, dockParams)
        dockView = layout
    }

    private fun dockBtn(label: String, dp: Float, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 22f
            setBackgroundColor(0xFF1E293B.toInt())
            setTextColor(0xFFF1F5F9.toInt())
            val pad = (10 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = (6 * dp).toInt() }
            setOnClickListener { onClick() }
        }

    private fun cycleAlpha(view: View) {
        dockAlpha = when {
            dockAlpha > 0.79f -> 0.55f
            dockAlpha > 0.49f -> 0.30f
            dockAlpha > 0.24f -> 0.10f
            else -> 0.85f
        }
        view.alpha = dockAlpha
    }

    private fun showDismissTarget() {
        val wm = windowManager ?: return
        if (dismissTarget != null) return
        val dp = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = "✕  drop to dismiss"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD991B1B.toInt())
            gravity = Gravity.CENTER
            alpha = 0.6f
            val pad = (16 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (40 * dp).toInt()
        }
        runCatching { wm.addView(tv, params) }
        dismissTarget = tv
    }

    private fun hideDismissTarget() {
        dismissTarget?.let { runCatching { windowManager?.removeView(it) } }
        dismissTarget = null
    }

    private fun removeDock() {
        hideDismissTarget()
        dockView?.let { runCatching { windowManager?.removeView(it) } }
        dockView = null
    }

    // ── Mic ───────────────────────────────────────────────────────────────────

    private fun handleMic() {
        if (isRecording) { stopMicAndProcess(); return }
        isRecording = true
        scope.launch {
            if (app.audioRecorder.start().isFailure) { isRecording = false; return@launch }
            kotlinx.coroutines.delay(8_000L)
            if (isRecording) stopMicAndProcess()
        }
    }

    private fun stopMicAndProcess() {
        if (!isRecording) return
        isRecording = false
        scope.launch {
            val pcm = app.audioRecorder.stop().getOrElse {
                Log.w(TAG, "mic stop failed: ${it.message}"); return@launch
            }
            if (pcm.isEmpty()) return@launch
            val transcript = app.transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
            if (transcript.isBlank() || transcript.startsWith("[")) {
                Log.w(TAG, "Transcription failed: $transcript"); return@launch
            }
            val jpeg = app.pendingScreenJpeg.value
            if (jpeg != null) {
                app.pendingScreenJpeg.value = null
                app.screenAsk(jpeg, transcript)
            } else {
                val node = focusedEditNode
                if (node != null) pasteText(node, transcript) else app.sendChat(transcript)
            }
        }
    }

    // ── Vision ────────────────────────────────────────────────────────────────

    private fun handleVision() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) captureAndStore()
        else Log.w(TAG, "takeScreenshot requires API 30+")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureAndStore() {
        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    scope.launch(Dispatchers.Default) {
                        val hw = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer, screenshot.colorSpace
                        ) ?: run {
                            Log.e(TAG, "wrapHardwareBuffer null")
                            screenshot.hardwareBuffer.close(); return@launch
                        }
                        val bmp = hw.copy(Bitmap.Config.ARGB_8888, false)
                        hw.recycle(); screenshot.hardwareBuffer.close()
                        val longest = maxOf(bmp.width, bmp.height)
                        val scaled = if (longest > 1024) {
                            val s = 1024f / longest
                            Bitmap.createScaledBitmap(
                                bmp,
                                (bmp.width * s).toInt().coerceAtLeast(1),
                                (bmp.height * s).toInt().coerceAtLeast(1),
                                true,
                            ).also { bmp.recycle() }
                        } else bmp
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        scaled.recycle()
                        app.pendingScreenJpeg.value = out.toByteArray()
                        Log.i(TAG, "Screenshot stored (${out.size()} bytes)")
                    }
                }
                override fun onFailure(errorCode: Int) {
                    Log.e(TAG, "takeScreenshot failed: $errorCode")
                }
            })
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    private fun handleStop() {
        app.stopAll()
        app.pendingScreenJpeg.value = null
        isRecording = false
    }

    // ── Paste ─────────────────────────────────────────────────────────────────

    private fun pasteText(node: AccessibilityNodeInfo, text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("horizons", text))
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    companion object { const val TAG = "HorizonsA11y" }
}
