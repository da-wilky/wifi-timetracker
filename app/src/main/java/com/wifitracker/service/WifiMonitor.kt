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

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String?
)

@Singleton
class WifiMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {
    fun observeWifiNetwork(): Flow<WifiNetworkInfo?> = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                val info = wifiInfo?.let {
                    val parsedSsid = it.ssid.removeSurrounding("\"")
                    // Android returns "<unknown ssid>" when SSID is unavailable
                    if (parsedSsid == "<unknown ssid>") {
                        null
                    } else {
                        WifiNetworkInfo(
                            ssid = parsedSsid,
                            bssid = it.bssid
                        )
                    }
                }
                trySend(info)
            }

            override fun onLost(network: Network) {
                trySend(null)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
