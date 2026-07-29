package com.fuerz4.assistant.data.voice.wakeword

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class MicOwner { NONE, WAKE_WORD, SPEECH_RECOGNIZER, TTS }

/**
 * Only one component may hold the microphone/audio output at a time: wake-word listening,
 * an active [com.fuerz4.assistant.data.voice.SpeechRecognizerManager] capture, or TTS playback.
 * [VoskWakeWordService] pauses its `AudioRecord` loop while it doesn't own the mic, and resumes
 * once the holder releases it — see CLAUDE.md for the full arbitration rule.
 */
@Singleton
class MicArbiter @Inject constructor() {
    private val _owner = MutableStateFlow(MicOwner.NONE)
    val owner: StateFlow<MicOwner> = _owner.asStateFlow()

    fun tryAcquire(requester: MicOwner): Boolean {
        return _owner.compareAndSet(MicOwner.NONE, requester) || _owner.value == requester
    }

    fun release(requester: MicOwner) {
        _owner.compareAndSet(requester, MicOwner.NONE)
    }
}
