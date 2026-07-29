package com.fuerz4.assistant.data.provisioning

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class WifiConnectResult {
    data class Success(val network: Network) : WifiConnectResult()
    data object Unavailable : WifiConnectResult()
    data object Timeout : WifiConnectResult()
}

/**
 * Connects the phone to a device's own SoftAP (`nUdpWiFi`) using `WifiNetworkSpecifier` so
 * provisioning traffic can be routed there even while the phone's regular WiFi stays configured,
 * then releases that request so the OS falls back to whatever it was already connected to.
 *
 * Platform constraint (see CLAUDE.md): since API 29+, apps cannot force-reconnect to an
 * arbitrary previous SSID — the original connection is never actually torn down at the OS level
 * while this specifier request is held, only deprioritized for this process's routing. Releasing
 * the request is what "restores" it; there's no API to guarantee or verify that reconnection
 * completes within a bounded time, so [releaseAndRestore] is best-effort by design.
 */
@Singleton
class WifiProvisioningManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var activeCallback: ConnectivityManager.NetworkCallback? = null

    suspend fun connectToDeviceAp(ssid: String, password: String, timeoutMs: Long = 25_000): WifiConnectResult {
        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build()

                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        connectivityManager.bindProcessToNetwork(network)
                        if (continuation.isActive) continuation.resume(WifiConnectResult.Success(network))
                    }

                    override fun onUnavailable() {
                        if (continuation.isActive) continuation.resume(WifiConnectResult.Unavailable)
                    }
                }

                activeCallback = callback
                continuation.invokeOnCancellation {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
                connectivityManager.requestNetwork(request, callback)
            }
        }

        return result ?: WifiConnectResult.Timeout
    }

    /** Releases the device-AP request and unbinds process routing; best-effort per the class doc. */
    suspend fun releaseAndRestore(verifyTimeoutMs: Long = 15_000) {
        activeCallback?.let { runCatching { connectivityManager.unregisterNetworkCallback(it) } }
        activeCallback = null
        connectivityManager.bindProcessToNetwork(null)

        withTimeoutOrNull(verifyTimeoutMs) {
            while (!hasDefaultInternetConnection()) {
                delay(500)
            }
        }
    }

    private fun hasDefaultInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
