package com.cortex.ai.agent.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class CortexVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle): VoiceInteractionSession =
        CortexVoiceInteractionSession(this)
}
