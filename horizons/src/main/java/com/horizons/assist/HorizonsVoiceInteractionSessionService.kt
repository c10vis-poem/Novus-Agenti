package com.horizons.assist

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Factory for [HorizonsVoiceInteractionSession]. The system creates a fresh
 * session for each assist invocation.
 */
class HorizonsVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return HorizonsVoiceInteractionSession(this)
    }
}
