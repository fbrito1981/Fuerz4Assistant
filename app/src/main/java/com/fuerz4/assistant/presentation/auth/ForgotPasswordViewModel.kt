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

enum class ForgotPasswordStep { REQUEST_CODE, VALIDATE_CODE, RESET_PASSWORD, DONE }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.REQUEST_CODE,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value, error = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, error = null) }

    fun sendCode() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.forgot_validation_error)) }
            return
        }
        runGuarded {
            authRepository.requestRecoveryCode(state.email.trim())
                .onSuccessUpdate { it.copy(step = ForgotPasswordStep.VALIDATE_CODE) }
        }
    }

    fun validateCode() {
        val state = _uiState.value
        val codeInt = state.code.trim().toIntOrNull()
        if (codeInt == null) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.forgot_validation_error)) }
            return
        }
        runGuarded {
            authRepository.validateRecoveryCode(state.email.trim(), codeInt)
                .onSuccessUpdate { it.copy(step = ForgotPasswordStep.RESET_PASSWORD) }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.newPassword.isBlank()) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.forgot_validation_error)) }
            return
        }
        runGuarded {
            authRepository.resetPassword(state.email.trim(), state.newPassword)
                .onSuccessUpdate { it.copy(step = ForgotPasswordStep.DONE) }
        }
    }

    private fun runGuarded(block: suspend () -> Result<Unit>) {
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            block().onFailure { throwable ->
                val uiText = (throwable as? NanoApiError)?.toUiText()
                    ?: UiText.Resource(R.string.common_error_general)
                _uiState.update { it.copy(error = uiText) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private inline fun Result<Unit>.onSuccessUpdate(crossinline transform: (ForgotPasswordUiState) -> ForgotPasswordUiState): Result<Unit> {
        onSuccess { _uiState.update(transform) }
        return this
    }
}
