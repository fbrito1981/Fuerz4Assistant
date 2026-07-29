package com.fuerz4.assistant.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class TtsState {
    data object Idle : TtsState()
    data object Speaking : TtsState()
    data object LanguageUnavailable : TtsState()
}

/** Thin wrapper around Android's native `TextToSpeech` — spoken replies for voice-triggered chat turns. */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var isReady = false

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onEngineReady()
        }
    }

    private fun onEngineReady() {
        val result = tts.setLanguage(Locale("es", "AR"))
        isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!isReady) {
            _state.value = TtsState.LanguageUnavailable
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Idle
            }
        })
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts.stop()
        _state.value = TtsState.Idle
    }

    fun shutdown() {
        tts.shutdown()
    }
}
