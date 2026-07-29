package com.fuerz4.assistant.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.domain.repository.AuthRepository
import com.fuerz4.assistant.presentation.common.UiText
import com.fuerz4.assistant.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val successMessage: UiText? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.name.isBlank() || state.email.isBlank()) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.register_validation_error)) }
            return
        }

        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.register(state.email.trim(), state.name.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = UiText.Resource(R.string.register_success_message))
                    }
                    onSuccess()
                }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText()
                        ?: UiText.Resource(R.string.common_error_general)
                    _uiState.update { it.copy(isLoading = false, error = uiText) }
                }
        }
    }
}
