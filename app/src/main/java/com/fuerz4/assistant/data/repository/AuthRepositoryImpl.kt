package com.fuerz4.assistant.data.repository

import com.fuerz4.assistant.data.crypto.SecureUtil
import com.fuerz4.assistant.data.remote.NanoApi
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.remote.dto.AuthResultDto
import com.fuerz4.assistant.data.remote.dto.LoginDto
import com.fuerz4.assistant.data.remote.dto.RegistrationDto
import com.fuerz4.assistant.data.remote.dto.TokenDto
import com.fuerz4.assistant.data.remote.dto.UserDto
import com.fuerz4.assistant.data.remote.dto.ValidateCodeDto
import com.fuerz4.assistant.data.remote.safeApiCall
import com.fuerz4.assistant.data.remote.safeApiCallBody
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.domain.model.User
import com.fuerz4.assistant.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

private const val LOGIN_TOKEN_HEADER = "LoginToken"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: NanoApi,
    private val session: SessionManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override val loginToken: StateFlow<String?> = session.loginToken
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    override val isLoggedIn: Boolean get() = session.isLoggedIn

    override suspend fun login(email: String, password: String): Result<User> {
        val encryptedData = SecureUtil.encrypt(LoginDto(email = email, pass = password))
        return safeApiCall { api.login(encryptedData) }.mapCatching { response ->
            response.toUser()
        }
    }

    override suspend fun tokenLogin(): Result<User> {
        val token = session.loginToken.value ?: return Result.failure(NanoApiError.Unknown())
        val encryptedData = SecureUtil.encrypt(TokenDto(token = token))
        return safeApiCall { api.tokenLogin(encryptedData) }.mapCatching { response ->
            response.toUser()
        }
    }

    override suspend fun register(email: String, name: String): Result<Unit> {
        val encryptedData = SecureUtil.encrypt(RegistrationDto(user = UserDto(email = email, name = name)))
        return safeApiCallBody { api.registerUser(encryptedData, images = "{}") }.map { }
    }

    override suspend fun requestRecoveryCode(email: String): Result<Unit> {
        val encryptedData = SecureUtil.encrypt(LoginDto(email = email, pass = ""))
        return safeApiCallBody { api.requestCode(encryptedData) }.map { }
    }

    override suspend fun validateRecoveryCode(email: String, code: Int): Result<Unit> {
        val encryptedData = SecureUtil.encrypt(ValidateCodeDto(email = email, code = code))
        return safeApiCallBody { api.validateCode(encryptedData) }.map { }
    }

    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val encryptedData = SecureUtil.encrypt(LoginDto(email = email, pass = newPassword))
        return safeApiCallBody { api.resetPassword(encryptedData) }.map { }
    }

    override fun logout() {
        session.clear()
        _currentUser.value = null
    }

    override fun updateCurrentUser(user: User) {
        _currentUser.value = user
    }

    private fun Response<AuthResultDto>.toUser(): User {
        val token = headers()[LOGIN_TOKEN_HEADER] ?: throw NanoApiError.Unknown()
        val encryptedUser = body()?.data ?: throw NanoApiError.Unknown()
        val userDto = SecureUtil.decrypt<UserDto>(encryptedUser)
        session.saveToken(token)
        val user = User(email = userDto.email.orEmpty(), name = userDto.name, picture = userDto.picture)
        _currentUser.value = user
        return user
    }
}
