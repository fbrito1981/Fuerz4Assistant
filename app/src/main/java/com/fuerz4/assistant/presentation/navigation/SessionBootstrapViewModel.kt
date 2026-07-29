package com.fuerz4.assistant.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * When the app cold-starts with a persisted `LoginToken` (see [com.fuerz4.assistant.data.session.SessionManager]),
 * only the token survives the process restart — the in-memory [AuthRepository.currentUser] does not.
 * This re-derives it once via `tokenLogin`, which also incidentally revalidates the token.
 */
@HiltViewModel
class SessionBootstrapViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    init {
        if (authRepository.isLoggedIn && authRepository.currentUser.value == null) {
            viewModelScope.launch {
                authRepository.tokenLogin()
            }
        }
    }
}
