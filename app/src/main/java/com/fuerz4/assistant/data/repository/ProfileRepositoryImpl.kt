package com.fuerz4.assistant.data.repository

import com.fuerz4.assistant.data.crypto.SecureUtil
import com.fuerz4.assistant.data.remote.NanoApi
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.remote.dto.UserDto
import com.fuerz4.assistant.data.remote.safeApiCallBody
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.domain.model.User
import com.fuerz4.assistant.domain.repository.AuthRepository
import com.fuerz4.assistant.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: NanoApi,
    private val session: SessionManager,
    private val authRepository: AuthRepository
) : ProfileRepository {

    override suspend fun updateName(name: String): Result<User> {
        val token = session.loginToken.value ?: return Result.failure(NanoApiError.Unknown())
        val encryptedData = SecureUtil.encrypt(UserDto(name = name))

        return safeApiCallBody { api.updateUser(token, encryptedData, images = "{}") }.map {
            val current = authRepository.currentUser.value
            val updated = (current ?: User(email = "", name = null, picture = null)).copy(name = name)
            authRepository.updateCurrentUser(updated)
            updated
        }
    }
}
