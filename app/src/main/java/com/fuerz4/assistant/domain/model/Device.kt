package com.fuerz4.assistant.domain.model

data class Device(
    val uuid: String,
    val name: String,
    val model: String?,
    val os: String?,
    val version: String?,
    val type: DeviceType?,
    val active: Boolean?,
    val created: Long?
)
