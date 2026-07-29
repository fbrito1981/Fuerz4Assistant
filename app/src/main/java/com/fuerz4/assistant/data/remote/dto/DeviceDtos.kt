package com.fuerz4.assistant.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val uuid: String,
    val name: String,
    val model: String? = null,
    val os: String? = null,
    val version: String? = null,
    val type: String? = null,
    val active: Boolean? = null,
    /** Epoch millis — the server's `Device.created` is a `java.util.Date`, and Jackson's default (un-annotated) serialization for that is a numeric timestamp, not an ISO string. */
    val created: Long? = null
)

@Serializable
data class DeviceListDto(val devices: List<DeviceDto> = emptyList())

@Serializable
data class DeviceListRequestDto(val type: String? = null)

@Serializable
data class DeviceRemoveRequestDto(val uuid: String)

/** Response envelope for the plain-JSON (unencrypted) Devices/Chat endpoints. */
@Serializable
data class DeviceListResultDto(
    val success: Boolean,
    val data: DeviceListDto? = null,
    val message: String? = null
)

@Serializable
data class DeviceResultDto(
    val success: Boolean,
    val data: DeviceDto? = null,
    val message: String? = null
)

@Serializable
data class SimpleResultDto(
    val success: Boolean,
    val message: String? = null
)
