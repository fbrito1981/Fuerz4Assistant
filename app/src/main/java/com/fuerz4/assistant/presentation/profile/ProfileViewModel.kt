package com.fuerz4.assistant.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.domain.repository.AuthRepository
import com.fuerz4.assistant.domain.repository.ProfileRepository
import com.fuerz4.assistant.presentation.common.UiText
import com.fuerz4.assistant.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val name: String = "",
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val successMessage: UiText? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(email = user?.email.orEmpty(), name = user?.name.orEmpty()) }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null, successMessage = null) }
    }

    fun save() {
        val name = _uiState.value.name
        if (name.isBlank()) return

        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }

            profileRepository.updateName(name.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, successMessage = UiText.Resource(R.string.profile_save_success))
                    }
                }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText()
                        ?: UiText.Resource(R.string.common_error_general)
                    _uiState.update { it.copy(isSaving = false, error = uiText) }
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        authRepository.logout()
        onLoggedOut()
    }
}
