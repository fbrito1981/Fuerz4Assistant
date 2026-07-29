package com.fuerz4.assistant.domain.repository

import com.fuerz4.assistant.domain.model.ChatMessage

interface ChatRepository {
    /** Sends [history] (already capped/trimmed by the caller) plus the new [message] and returns the assistant's reply text. */
    suspend fun sendMessage(history: List<ChatMessage>, message: String): Result<String>
}
