package com.fuerz4.assistant.domain.repository

import com.fuerz4.assistant.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val loginToken: StateFlow<String?>
    val currentUser: StateFlow<User?>
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Result<User>
    suspend fun tokenLogin(): Result<User>
    suspend fun register(email: String, name: String): Result<Unit>
    suspend fun requestRecoveryCode(email: String): Result<Unit>
    suspend fun validateRecoveryCode(email: String, code: Int): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
    fun logout()

    /** Lets other repositories (e.g. Profile) refresh the cached user after a successful update. */
    fun updateCurrentUser(user: User)
}
