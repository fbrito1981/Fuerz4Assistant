package com.fuerz4.assistant.data.voice.wakeword

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lets [VoskWakeWordService] (running in the background) notify the UI layer when "Che fuerza" was heard. */
@Singleton
class WakeWordEventBus @Inject constructor() {
    private val _detected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val detected: SharedFlow<Unit> = _detected.asSharedFlow()

    fun notifyDetected() {
        _detected.tryEmit(Unit)
    }
}
