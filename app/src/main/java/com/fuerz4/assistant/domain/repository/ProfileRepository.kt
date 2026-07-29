package com.fuerz4.assistant.domain.repository

import com.fuerz4.assistant.domain.model.User

interface ProfileRepository {
    suspend fun updateName(name: String): Result<User>
}
