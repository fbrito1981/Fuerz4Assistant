package com.fuerz4.assistant.data.repository

import com.fuerz4.assistant.data.remote.NanoApi
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.remote.dto.DeviceDto
import com.fuerz4.assistant.data.remote.dto.DeviceListRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceRemoveRequestDto
import com.fuerz4.assistant.data.remote.safeApiCallBody
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.domain.model.Device
import com.fuerz4.assistant.domain.model.DeviceType
import com.fuerz4.assistant.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: NanoApi,
    private val session: SessionManager
) : DeviceRepository {

    override suspend fun listDevices(type: DeviceType?): Result<List<Device>> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val wireType = type?.let { if (it == DeviceType.ENERGY) "energy" else "environment" }

        return safeApiCallBody { api.listDevices(token, DeviceListRequestDto(type = wireType)) }
            .map { result -> result.data?.devices.orEmpty().map { dto -> dto.toDomain() } }
    }

    override suspend fun createDevice(uuid: String, name: String, model: String?): Result<Device> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val dto = DeviceDto(uuid = uuid, name = name, model = model)

        return safeApiCallBody { api.createDevice(token, dto) }.mapCatching { result ->
            (result.data ?: throw NanoApiError.Unknown()).toDomain()
        }
    }

    override suspend fun updateDevice(uuid: String, name: String, active: Boolean?): Result<Device> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val dto = DeviceDto(uuid = uuid, name = name, active = active)

        return safeApiCallBody { api.updateDevice(token, dto) }.mapCatching { result ->
            (result.data ?: throw NanoApiError.Unknown()).toDomain()
        }
    }

    override suspend fun removeDevice(uuid: String): Result<Unit> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())

        return safeApiCallBody { api.removeDevice(token, DeviceRemoveRequestDto(uuid = uuid)) }.map { }
    }

    private fun token(): String? = session.loginToken.value

    private fun DeviceDto.toDomain() = Device(
        uuid = uuid,
        name = name,
        model = model,
        os = os,
        version = version,
        type = DeviceType.fromWireValue(type) ?: DeviceType.fromUuid(uuid),
        active = active,
        created = created
    )
}
