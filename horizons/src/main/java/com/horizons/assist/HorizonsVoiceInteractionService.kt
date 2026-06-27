package com.horizons.assist

import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * Top-level system binding for the Assist API path. Activated when the user
 * selects Horizons as the default Digital Assistant in system settings.
 *
 * Once active, the system routes assist gestures (long-press home, "Hey
 * Assistant", power-button hold) to our [HorizonsVoiceInteractionSessionService]
 * which produces a [HorizonsVoiceInteractionSession] — that session receives
 * the foreground app's view tree and screenshot WITHOUT a MediaProjection
 * consent prompt.
 */
class HorizonsVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i(TAG, "VoiceInteractionService ready")
    }

    companion object {
        private const val TAG = "HorizonsVIService"
    }
}
