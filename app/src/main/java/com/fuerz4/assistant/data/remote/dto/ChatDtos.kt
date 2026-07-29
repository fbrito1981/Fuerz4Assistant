package com.fuerz4.assistant.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatTurnDto(val role: String, val text: String)

@Serializable
data class ChatRequestDto(val history: List<ChatTurnDto>, val message: String)

@Serializable
data class ChatResponseDto(val reply: String)

@Serializable
data class ChatResultDto(
    val success: Boolean,
    val data: ChatResponseDto? = null,
    val message: String? = null
)
