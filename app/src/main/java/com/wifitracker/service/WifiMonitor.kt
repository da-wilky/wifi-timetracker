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

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String?
)

@Singleton
class WifiMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {
    companion object {
        private const val TAG = "WifiMonitor"
    }

    fun observeWifiNetwork(): Flow<WifiNetworkInfo?> = callbackFlow {
        // Emit initial state immediately
        val activeNetwork = connectivityManager.activeNetwork
        val activeCapabilities = activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }
        if (activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifiInfo = activeCapabilities.transportInfo as? WifiInfo
            Log.d(TAG, "Initial WiFi check: wifiInfo=$wifiInfo, ssid=${wifiInfo?.ssid}, bssid=${wifiInfo?.bssid}")
            val info = wifiInfo?.let {
                val parsedSsid = it.ssid.removeSurrounding("\"")
                if (parsedSsid == "<unknown ssid>") {
                    Log.w(TAG, "Received sentinel '<unknown ssid>' - likely missing location permission")
                    null
                } else {
                    Log.i(TAG, "WiFi network detected: ssid=$parsedSsid, bssid=${it.bssid}")
                    WifiNetworkInfo(
                        ssid = parsedSsid,
                        bssid = it.bssid
                    )
                }
            }
            trySend(info)
        } else {
            Log.d(TAG, "No active WiFi transport")
            trySend(null)
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                Log.d(TAG, "Network capabilities changed: wifiInfo=$wifiInfo, ssid=${wifiInfo?.ssid}, bssid=${wifiInfo?.bssid}")
                val info = wifiInfo?.let {
                    val parsedSsid = it.ssid.removeSurrounding("\"")
                    // Android returns "<unknown ssid>" when SSID is unavailable
                    if (parsedSsid == "<unknown ssid>") {
                        Log.w(TAG, "Received sentinel '<unknown ssid>' in callback - likely missing location permission")
                        null
                    } else {
                        Log.i(TAG, "WiFi network update: ssid=$parsedSsid, bssid=${it.bssid}")
                        WifiNetworkInfo(
                            ssid = parsedSsid,
                            bssid = it.bssid
                        )
                    }
                }
                trySend(info)
            }

            override fun onLost(network: Network) {
                Log.i(TAG, "WiFi network lost")
                trySend(null)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
