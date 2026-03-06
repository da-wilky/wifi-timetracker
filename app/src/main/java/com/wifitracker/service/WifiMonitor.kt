package com.wifitracker.service

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the possible WiFi network states observed by [WifiMonitor].
 *
 * Differentiating between [Disconnected] and [SsidUnavailable] is important:
 * when Location Services are disabled Android returns the sentinel `<unknown ssid>`
 * instead of the real SSID even though the device is still physically connected.
 * In that case we must NOT record a DISCONNECT event.
 */
sealed class WifiNetworkState {
    /** The device is not connected to any WiFi network. */
    data object Disconnected : WifiNetworkState()

    /**
     * The device is connected to WiFi but the SSID cannot be read because
     * Location Services are disabled or the required permission is absent.
     */
    data object SsidUnavailable : WifiNetworkState()

    /** The device is connected to a WiFi network whose SSID and BSSID are known. */
    data class Connected(val ssid: String, val bssid: String?) : WifiNetworkState()
}

@Singleton
class WifiMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {
    companion object {
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }

    fun observeWifiNetwork(): Flow<WifiNetworkState> = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onAvailable(network: Network) {
                // Capture initial connection state when the service first observes the network
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val wifiInfo = capabilities?.transportInfo as? WifiInfo
                trySend(parseWifiInfo(wifiInfo))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                trySend(parseWifiInfo(wifiInfo))
            }

            override fun onLost(network: Network) {
                trySend(WifiNetworkState.Disconnected)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun parseWifiInfo(wifiInfo: WifiInfo?): WifiNetworkState {
        if (wifiInfo == null) return WifiNetworkState.Disconnected
        val parsedSsid = wifiInfo.ssid.removeSurrounding("\"")
        return if (parsedSsid == UNKNOWN_SSID) {
            // Android returns this sentinel when Location Services are off.
            // The device is still connected to the same network – do NOT treat this as a disconnect.
            WifiNetworkState.SsidUnavailable
        } else {
            WifiNetworkState.Connected(ssid = parsedSsid, bssid = wifiInfo.bssid)
        }
    }
}
