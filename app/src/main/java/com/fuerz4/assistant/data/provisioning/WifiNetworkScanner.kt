package com.fuerz4.assistant.data.provisioning

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val BAND_24_GHZ_MIN_MHZ = 2400
private const val BAND_24_GHZ_MAX_MHZ = 2500

/**
 * Reads the system's cached WiFi scan results for the network-picker in [DeviceFormScreen].
 * Uses [WifiManager.getScanResults] (last scan the OS already performed) rather than driving our
 * own [WifiManager.startScan] loop, since startScan is throttled to a few calls per app per
 * 2-minute window on API 28+ and often blocked outright by OEMs — [requestScan] is a best-effort
 * nudge, not something callers should depend on completing before reading results back.
 *
 * Both calls require the ACCESS_FINE_LOCATION runtime permission (declared in the manifest) to
 * return real SSIDs instead of an empty/redacted list; wrapped in [runCatching] because some OEM
 * builds throw [SecurityException] here even when the permission looks granted.
 */
@Singleton
class WifiNetworkScanner @Inject constructor(
    @ApplicationContext context: Context
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Suppress("DEPRECATION")
    fun requestScan() {
        runCatching { wifiManager.startScan() }
    }

    /** SSIDs from the last scan, 2.4GHz-only when the band info is available, strongest first. */
    @Suppress("DEPRECATION")
    fun cachedSsids(): List<String> = runCatching {
        val results = wifiManager.scanResults.orEmpty()
        val band24 = results.filter { it.frequency in BAND_24_GHZ_MIN_MHZ..BAND_24_GHZ_MAX_MHZ }
        val candidates = band24.ifEmpty { results }

        candidates
            .filter { it.SSID.isNotBlank() }
            .sortedByDescending { it.level }
            .map { it.SSID }
            .distinct()
    }.getOrDefault(emptyList())
}
