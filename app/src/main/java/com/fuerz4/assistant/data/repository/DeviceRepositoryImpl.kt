package com.fuerz4.assistant.data.repository

import com.fuerz4.assistant.data.remote.NanoApi
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.data.remote.dto.DeviceDto
import com.fuerz4.assistant.data.remote.dto.DeviceHistoryRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceLatestRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceListRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceReadingDto
import com.fuerz4.assistant.data.remote.dto.DeviceRemoveRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceSettingsDto
import com.fuerz4.assistant.data.remote.safeApiCallBody
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.domain.model.Device
import com.fuerz4.assistant.domain.model.DeviceReading
import com.fuerz4.assistant.domain.model.DeviceSettings
import com.fuerz4.assistant.domain.model.DeviceType
import com.fuerz4.assistant.domain.repository.DeviceRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Matches NanoServer's `ChatWebService.DATE_PATTERNS[0]` / `DeviceHistoryWebService`'s lenient parser. */
private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

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

    override suspend fun createDevice(uuid: String, name: String, model: String?, settings: DeviceSettings?): Result<Device> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val dto = DeviceDto(uuid = uuid, name = name, model = model, settings = settings?.toDto())

        return safeApiCallBody { api.createDevice(token, dto) }.mapCatching { result ->
            (result.data ?: throw NanoApiError.Unknown()).toDomain()
        }
    }

    override suspend fun updateDevice(uuid: String, name: String, active: Boolean?, settings: DeviceSettings?): Result<Device> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val dto = DeviceDto(uuid = uuid, name = name, active = active, settings = settings?.toDto())

        return safeApiCallBody { api.updateDevice(token, dto) }.mapCatching { result ->
            (result.data ?: throw NanoApiError.Unknown()).toDomain()
        }
    }

    override suspend fun removeDevice(uuid: String): Result<Unit> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())

        return safeApiCallBody { api.removeDevice(token, DeviceRemoveRequestDto(uuid = uuid)) }.map { }
    }

    override suspend fun getLatestReading(uuid: String): Result<DeviceReading?> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())

        return safeApiCallBody { api.latestDeviceReading(token, DeviceLatestRequestDto(deviceUuid = uuid)) }
            .map { result -> result.data?.toDomain() }
    }

    override suspend fun getHistory(uuid: String, from: Long?, until: Long?, viewType: String): Result<List<DeviceReading>> {
        val token = token() ?: return Result.failure(NanoApiError.Unknown())
        val body = DeviceHistoryRequestDto(
            deviceUuid = uuid,
            fromDate = from?.let { formatIsoUtc(it) },
            untilDate = until?.let { formatIsoUtc(it) },
            viewType = viewType
        )

        return safeApiCallBody { api.deviceHistory(token, body) }
            .map { result -> result.data?.logs.orEmpty().map { dto -> dto.toDomain() } }
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
        settings = settings?.toDomain(),
        created = created
    )

    private fun DeviceSettings.toDto() = DeviceSettingsDto(ssid = ssid, volts = volts, amps = amps, temp = temp, hum = hum)

    private fun DeviceSettingsDto.toDomain() = DeviceSettings(ssid = ssid, volts = volts, amps = amps, temp = temp, hum = hum)

    private fun DeviceReadingDto.toDomain() = DeviceReading(
        timestamp = created ?: 0L,
        volts = volts,
        amps = amps,
        temp = temp,
        hum = hum,
        frequency = frequency,
        cosPhi = cosPhi,
        activePower = activePower
    )

    private fun formatIsoUtc(epochMillis: Long): String =
        isoDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC))
}
