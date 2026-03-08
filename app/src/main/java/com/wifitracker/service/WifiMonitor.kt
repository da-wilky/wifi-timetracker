package com.wifitracker.service

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.util.Log
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
        private const val TAG = "WifiMonitor"
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }

    /**
     * Returns the current WiFi state by querying [ConnectivityManager] directly.
     *
     * Unlike [observeWifiNetwork], this performs a one-shot synchronous check and is
     * suitable for on-demand refresh (e.g. after location permissions are granted).
     */
    fun getCurrentState(): WifiNetworkState {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            Log.d(TAG, "getCurrentState() -> Disconnected (no active network)")
            return WifiNetworkState.Disconnected
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            Log.d(TAG, "getCurrentState() -> Disconnected (no network capabilities)")
            return WifiNetworkState.Disconnected
        }

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.d(TAG, "getCurrentState() -> Disconnected (not WiFi transport)")
            return WifiNetworkState.Disconnected
        }

        val wifiInfo = capabilities.transportInfo as? WifiInfo
        val state = parseWifiInfo(wifiInfo)

        when (state) {
            is WifiNetworkState.Connected ->
                Log.d(TAG, "getCurrentState() -> Connected(ssid=${state.ssid}, bssid=${state.bssid})")
            is WifiNetworkState.SsidUnavailable ->
                Log.d(TAG, "getCurrentState() -> SsidUnavailable")
            is WifiNetworkState.Disconnected ->
                Log.d(TAG, "getCurrentState() -> Disconnected (no WiFi info)")
        }

        return state
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
                val state = parseWifiInfo(wifiInfo)
                Log.d(TAG, "observeWifiNetwork.onAvailable() -> $state")
                trySend(state)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                val state = parseWifiInfo(wifiInfo)
                Log.d(TAG, "observeWifiNetwork.onCapabilitiesChanged() -> $state")
                trySend(state)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "observeWifiNetwork.onLost() -> Disconnected")
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
