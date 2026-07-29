package com.fuerz4.assistant.data.provisioning

import android.net.Network
import com.fuerz4.assistant.domain.model.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Locale
import javax.inject.Inject

sealed class ProvisioningResult {
    data object Success : ProvisioningResult()
    data object Timeout : ProvisioningResult()
    data object EchoMismatch : ProvisioningResult()
}

/**
 * Direct Kotlin port of the UDP protocol proven out in
 * `CuatroBoomClt/src/toto/connection/UdpSender.java`: send a JSON config packet to the device's
 * SoftAP gateway, then block for its verbatim echo as an ack. That reference file has no Android
 * code (plain Java, no `android.*` imports) — only the wire protocol is reused here.
 */
class UdpProvisioningClient @Inject constructor() {

    suspend fun provision(
        network: Network,
        type: DeviceType,
        deviceId: String,
        homeSsid: String,
        homePass: String,
        volts: Float?,
        amps: Float?,
        temp: Float?,
        hum: Float?
    ): ProvisioningResult = withContext(Dispatchers.IO) {
        val message = buildPayload(type, deviceId, homeSsid, homePass, volts, amps, temp, hum)
        val payloadBytes = message.toByteArray(Charsets.UTF_8)

        val socket = DatagramSocket(DEVICE_PORT)
        try {
            network.bindSocket(socket)
            socket.soTimeout = SOCKET_TIMEOUT_MS

            val address = InetAddress.getByName(DEVICE_IP)
            socket.send(DatagramPacket(payloadBytes, payloadBytes.size, address, DEVICE_PORT))

            val receiveBuffer = ByteArray(RECEIVE_BUFFER_SIZE)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)

            val echoed = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
            if (echoed == message) ProvisioningResult.Success else ProvisioningResult.EchoMismatch
        } catch (e: java.net.SocketTimeoutException) {
            ProvisioningResult.Timeout
        } finally {
            socket.close()
        }
    }

    private fun buildPayload(
        type: DeviceType,
        deviceId: String,
        homeSsid: String,
        homePass: String,
        volts: Float?,
        amps: Float?,
        temp: Float?,
        hum: Float?
    ): String = when (type) {
        DeviceType.ENERGY -> String.format(
            Locale.US, ENERGY_TEMPLATE, homeSsid, homePass, deviceId, volts ?: 0f, amps ?: 0f
        )
        DeviceType.ENVIRONMENT -> String.format(
            Locale.US, ENVIRONMENT_TEMPLATE, homeSsid, homePass, deviceId, temp ?: 0f, hum ?: 0f
        )
    }

    private companion object {
        const val DEVICE_IP = "192.168.4.1"
        const val DEVICE_PORT = 4642
        const val SOCKET_TIMEOUT_MS = 10000
        const val RECEIVE_BUFFER_SIZE = 255
        const val ENERGY_TEMPLATE = "{\"ssid\":\"%s\",\"pass\":\"%s\",\"id\":\"%s\",\"volts\":%.2f,\"amps\":%.2f}"
        const val ENVIRONMENT_TEMPLATE = "{\"ssid\":\"%s\",\"pass\":\"%s\",\"id\":\"%s\",\"temp\":%.2f,\"hum\":%.2f}"
    }
}
