package com.fuerz4.assistant.domain.model

data class Device(
    val uuid: String,
    val name: String,
    val model: String?,
    val os: String?,
    val version: String?,
    val type: DeviceType?,
    val active: Boolean?,
    val settings: DeviceSettings?,
    val created: Long?
)

/**
 * Provisioned home-WiFi + sensor settings. Energía devices carry [volts]/[amps], Ambiente devices
 * carry [temp]/[hum]; [ssid] applies to both.
 */
data class DeviceSettings(
    val ssid: String?,
    val volts: Double?,
    val amps: Double?,
    val temp: Double?,
    val hum: Double?
)
