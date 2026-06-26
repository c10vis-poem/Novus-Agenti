package com.horizons.assist

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import com.horizons.HorizonsApplication
import java.io.ByteArrayOutputStream

/**
 * Receives the assist payload (view tree + screenshot) and routes it into the
 * existing Horizons screen-Q&A flow. This is the bypass — no MediaProjection
 * consent, no per-session permission prompt; the user pre-authorized us by
 * setting Horizons as the default assistant.
 */
class HorizonsVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val app: HorizonsApplication
        get() = context.applicationContext as HorizonsApplication

    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        Log.i(TAG, "onHandleAssist (structure=${state.assistStructure != null})")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        if (screenshot == null) {
            Log.w(TAG, "onHandleScreenshot: null bitmap")
            hide()
            return
        }
        val jpeg = ByteArrayOutputStream().use { baos ->
            screenshot.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.toByteArray()
        }
        app.screenAsk(jpeg, "What is on this screen?")
        hide()
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
    }

    companion object {
        private const val TAG = "HorizonsVISession"
    }
}
