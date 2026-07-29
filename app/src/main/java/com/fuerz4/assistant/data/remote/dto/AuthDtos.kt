package com.fuerz4.assistant.data.remote.dto

import kotlinx.serialization.Serializable

/** Envelope for the Login/Users family of endpoints, whose `data` is an encrypted string payload. */
@Serializable
data class AuthResultDto(
    val success: Boolean,
    val data: String? = null,
    val message: String? = null
)

@Serializable
data class LoginDto(val email: String, val pass: String)

@Serializable
data class TokenDto(val token: String)

@Serializable
data class ValidateCodeDto(val email: String, val code: Int)

@Serializable
data class UserDto(
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null,
    val active: Boolean? = null,
    val roleName: String? = null,
    val pictureRemoved: Boolean? = null
)

@Serializable
data class RegistrationDto(val user: UserDto)

@Serializable
data class UserImagesDto(
    val picture: String? = null,
    val pictureRemoved: Boolean? = null
)
