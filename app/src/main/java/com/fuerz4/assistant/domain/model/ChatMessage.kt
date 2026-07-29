package com.fuerz4.assistant.domain.model

enum class ChatRole { USER, ASSISTANT }

enum class InputMode { TEXT, VOICE }

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val timestamp: Long,
    val triggeredByVoice: Boolean = false
)
