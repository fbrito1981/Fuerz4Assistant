package com.fuerz4.assistant.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Unified shape for both server-side reading DTOs (`EnergyLogDto`/`EnvironmentLogDto`), which
 * differ by device type. Safe because [com.fuerz4.assistant.di.NetworkModule]'s shared
 * `Json { ignoreUnknownKeys = true }` discards whichever fields this class doesn't declare
 * (`device`, `difference`, `apparentPower`, `reactivePower`, etc.) — the same trick
 * [DeviceSettingsDto] already uses. `frequency`/`cosPhi`/`activePower` mirror `EnergyLogDto`'s
 * getters of the same name (Jackson's default bean-property naming).
 */
@Serializable
data class DeviceReadingDto(
    val created: Long? = null,
    val volts: Double? = null,
    val amps: Double? = null,
    val temp: Double? = null,
    val hum: Double? = null,
    val frequency: Double? = null,
    val cosPhi: Double? = null,
    val activePower: Double? = null
)

@Serializable
data class DeviceReadingsDto(val logs: List<DeviceReadingDto> = emptyList())

@Serializable
data class DeviceReadingResultDto(
    val success: Boolean,
    val data: DeviceReadingDto? = null,
    val message: String? = null
)

@Serializable
data class DeviceReadingsResultDto(
    val success: Boolean,
    val data: DeviceReadingsDto? = null,
    val message: String? = null
)

@Serializable
data class DeviceLatestRequestDto(val deviceUuid: String)

@Serializable
data class DeviceHistoryRequestDto(
    val deviceUuid: String,
    val fromDate: String? = null,
    val untilDate: String? = null,
    val viewType: String
)
