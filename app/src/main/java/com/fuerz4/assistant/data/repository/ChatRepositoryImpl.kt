package com.fuerz4.assistant.data.repository

import com.fuerz4.assistant.data.remote.NanoApi
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.remote.dto.ChatRequestDto
import com.fuerz4.assistant.data.remote.dto.ChatTurnDto
import com.fuerz4.assistant.data.remote.safeApiCallBody
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.domain.model.ChatMessage
import com.fuerz4.assistant.domain.model.ChatRole
import com.fuerz4.assistant.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: NanoApi,
    private val session: SessionManager
) : ChatRepository {

    override suspend fun sendMessage(history: List<ChatMessage>, message: String): Result<String> {
        val token = session.loginToken.value ?: return Result.failure(NanoApiError.Unknown())

        val turns = history.map {
            ChatTurnDto(role = if (it.role == ChatRole.USER) "user" else "assistant", text = it.text)
        }
        val request = ChatRequestDto(history = turns, message = message)

        return safeApiCallBody { api.converse(token, request) }.mapCatching { result ->
            result.data?.reply ?: throw NanoApiError.Unknown()
        }
    }
}
