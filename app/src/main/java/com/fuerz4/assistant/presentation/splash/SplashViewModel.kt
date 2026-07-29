package com.fuerz4.assistant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object GoHome : SplashDestination()
    data object GoLogin : SplashDestination()
}

/**
 * Only the `LoginToken` survives a cold start (see [com.fuerz4.assistant.data.session.SessionManager]) —
 * the in-memory [AuthRepository.currentUser] does not. This screen's job is to re-derive it via
 * `tokenLogin` (which also revalidates the token) before routing to Home/Login, so the rest of the
 * app never has to handle "logged in but no user loaded yet".
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            if (authRepository.isLoggedIn) {
                authRepository.tokenLogin()
                    .onSuccess { _destination.value = SplashDestination.GoHome }
                    .onFailure {
                        authRepository.logout()
                        _destination.value = SplashDestination.GoLogin
                    }
            } else {
                _destination.value = SplashDestination.GoLogin
            }
        }
    }
}
