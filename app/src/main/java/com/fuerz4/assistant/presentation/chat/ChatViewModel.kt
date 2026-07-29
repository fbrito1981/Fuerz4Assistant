package com.fuerz4.assistant.presentation.chat

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.voice.SpeechRecognizerManager
import com.fuerz4.assistant.data.voice.SpeechState
import com.fuerz4.assistant.data.voice.TextToSpeechManager
import com.fuerz4.assistant.data.voice.TtsState
import com.fuerz4.assistant.data.voice.wakeword.MicArbiter
import com.fuerz4.assistant.data.voice.wakeword.MicOwner
import com.fuerz4.assistant.data.voice.wakeword.VoskModelProvisioner
import com.fuerz4.assistant.data.voice.wakeword.VoskWakeWordService
import com.fuerz4.assistant.data.voice.wakeword.WakeWordEventBus
import com.fuerz4.assistant.domain.model.ChatMessage
import com.fuerz4.assistant.domain.model.ChatRole
import com.fuerz4.assistant.domain.model.InputMode
import com.fuerz4.assistant.domain.repository.ChatRepository
import com.fuerz4.assistant.presentation.common.UiText
import com.fuerz4.assistant.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val MAX_HISTORY_TURNS = 20

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isOffline: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val networkMonitor: NetworkMonitor,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val micArbiter: MicArbiter,
    private val voskModelProvisioner: VoskModelProvisioner,
    private val wakeWordEventBus: WakeWordEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        networkMonitor.isOnline
            .onEach { online -> _uiState.update { it.copy(isOffline = !online) } }
            .launchIn(viewModelScope)

        speechRecognizerManager.state
            .onEach(::handleSpeechState)
            .launchIn(viewModelScope)

        textToSpeechManager.state
            .onEach { state -> _uiState.update { it.copy(isSpeaking = state is TtsState.Speaking) } }
            .launchIn(viewModelScope)

        wakeWordEventBus.detected
            .onEach { startVoiceInput() }
            .launchIn(viewModelScope)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        send(text.trim(), InputMode.TEXT)
    }

    fun startVoiceInput() {
        if (!speechRecognizerManager.isAvailable()) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.chat_speech_not_available)) }
            return
        }
        if (!micArbiter.tryAcquire(MicOwner.SPEECH_RECOGNIZER)) return

        speechRecognizerManager.startListening()
    }

    fun clearConversation() {
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        if (!enabled) {
            context.stopService(Intent(context, VoskWakeWordService::class.java))
            _uiState.update { it.copy(wakeWordEnabled = false) }
            return
        }

        viewModelScope.launch {
            if (!voskModelProvisioner.isModelReady()) {
                _uiState.update { it.copy(isDownloadingModel = true) }

                voskModelProvisioner.downloadAndUnpackIfNeeded().onFailure {
                    _uiState.update {
                        it.copy(
                            isDownloadingModel = false,
                            error = UiText.Resource(R.string.chat_wake_word_download_error)
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isDownloadingModel = false) }
            }

            ContextCompat.startForegroundService(context, Intent(context, VoskWakeWordService::class.java))
            _uiState.update { it.copy(wakeWordEnabled = true) }
        }
    }

    private fun handleSpeechState(state: SpeechState) {
        when (state) {
            is SpeechState.Listening -> _uiState.update { it.copy(isListening = true, error = null) }
            is SpeechState.Result -> {
                micArbiter.release(MicOwner.SPEECH_RECOGNIZER)
                _uiState.update { it.copy(isListening = false) }
                send(state.text, InputMode.VOICE)
            }
            is SpeechState.Error -> {
                micArbiter.release(MicOwner.SPEECH_RECOGNIZER)
                _uiState.update { it.copy(isListening = false, error = UiText.Resource(R.string.chat_speech_error)) }
            }
            SpeechState.Idle -> _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun send(text: String, inputMode: InputMode) {
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = text,
            timestamp = System.currentTimeMillis(),
            triggeredByVoice = inputMode == InputMode.VOICE
        )
        _uiState.update { it.copy(messages = it.messages + userMessage, isSending = true, error = null) }

        viewModelScope.launch {
            val history = _uiState.value.messages.takeLast(MAX_HISTORY_TURNS)

            chatRepository.sendMessage(history, text)
                .onSuccess { reply ->
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.ASSISTANT,
                        text = reply,
                        timestamp = System.currentTimeMillis(),
                        triggeredByVoice = inputMode == InputMode.VOICE
                    )
                    _uiState.update { it.copy(messages = it.messages + assistantMessage, isSending = false) }

                    if (inputMode == InputMode.VOICE) {
                        textToSpeechManager.speak(reply)
                    }
                }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText()
                        ?: UiText.Resource(R.string.chat_error_sending)
                    _uiState.update { it.copy(isSending = false, error = uiText) }
                }
        }
    }

    override fun onCleared() {
        speechRecognizerManager.stopListening()
        textToSpeechManager.stop()
        super.onCleared()
    }
}
