package com.fuerz4.assistant.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechState {
    data object Idle : SpeechState()
    data object Listening : SpeechState()
    data class Result(val text: String) : SpeechState()
    data class Error(val code: Int) : SpeechState()
}

/** Thin wrapper around Android's native `SpeechRecognizer` — transcribes a single voice turn to text. Main-thread only. */
@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            _state.value = SpeechState.Error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            return
        }

        stopListening()

        val newRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        newRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                _state.value = if (!text.isNullOrBlank()) {
                    SpeechState.Result(text)
                } else {
                    SpeechState.Error(SpeechRecognizer.ERROR_NO_MATCH)
                }
            }

            override fun onError(error: Int) {
                _state.value = SpeechState.Error(error)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        recognizer = newRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        newRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.destroy()
        recognizer = null
        if (_state.value is SpeechState.Listening) {
            _state.value = SpeechState.Idle
        }
    }
}
