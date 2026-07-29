package com.fuerz4.assistant.data.provisioning

import com.fuerz4.assistant.domain.model.DeviceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import javax.inject.Inject

/**
 * Generates the UDP `id` field, which doubles as the device's server-side `uuid` — it must start
 * with the type prefix NanoServer's `Device.getType()` sniffs for (`WFEM`/`WFTM`), matching the
 * `yyyyMMddHHmmssSSS`-style timestamp id used by the reference `UdpSender.java`, plus a short
 * random suffix for extra uniqueness when provisioning two devices within the same millisecond.
 */
class DeviceIdGenerator @Inject constructor() {
    private val timestampFormat = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US)

    fun generate(type: DeviceType): String {
        val timestamp = timestampFormat.format(Date())
        val randomSuffix = Random.nextInt(0, 0xFFF).toString(16).padStart(3, '0')
        return "${type.prefix}$timestamp$randomSuffix"
    }
}
