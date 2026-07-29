package com.fuerz4.assistant.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Exposes whether the device currently has usable internet connectivity, for offline-state gating. */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    externalScope: kotlinx.coroutines.CoroutineScope
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .build()

    private fun observe(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val activeNetworks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                activeNetworks.add(network)
                trySend(activeNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                activeNetworks.remove(network)
                trySend(activeNetworks.isNotEmpty())
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(hasActiveConnection())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun hasActiveConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    val isOnline: StateFlow<Boolean> = observe()
        .stateIn(externalScope, SharingStarted.Eagerly, hasActiveConnection())

    /** Device provisioning specifically requires WiFi (not just any internet transport) since it must join the device's own AP. */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
