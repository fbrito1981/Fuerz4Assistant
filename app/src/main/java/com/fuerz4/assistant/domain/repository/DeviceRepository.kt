package com.fuerz4.assistant.domain.repository

import com.fuerz4.assistant.domain.model.Device
import com.fuerz4.assistant.domain.model.DeviceSettings
import com.fuerz4.assistant.domain.model.DeviceType

interface DeviceRepository {
    suspend fun listDevices(type: DeviceType? = null): Result<List<Device>>
    suspend fun createDevice(uuid: String, name: String, model: String?, settings: DeviceSettings? = null): Result<Device>
    suspend fun updateDevice(uuid: String, name: String, active: Boolean?, settings: DeviceSettings? = null): Result<Device>
    suspend fun removeDevice(uuid: String): Result<Unit>
}
