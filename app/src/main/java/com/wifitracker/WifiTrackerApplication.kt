package com.wifitracker

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import androidx.core.content.ContextCompat
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.data.repository.TrackerRepository
import com.wifitracker.service.WifiTrackingService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WifiTrackerApplication : Application() {

    @Inject
    lateinit var trackerRepository: TrackerRepository

    @Inject
    lateinit var eventDao: EventDao

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            performCrashRecovery()
        }
    }

    private suspend fun performCrashRecovery() {
        val isServiceRunning = isServiceRunning(WifiTrackingService::class.java)

        if (!isServiceRunning) {
            // Close orphaned CONNECT events
            val openConnects = eventDao.findOpenConnects()
            val currentTime = System.currentTimeMillis()

            for (openConnect in openConnects) {
                eventDao.insert(
                    EventEntity(
                        trackerId = openConnect.trackerId,
                        eventType = "DISCONNECT",
                        timestamp = currentTime
                    )
                )
            }

            if (openConnects.isNotEmpty()) {
                // Set flag to show warning
                getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("show_warning", true)
                    .apply()
            }

            // Insert CONNECT for currently connected network if tracked
            val currentNetwork = connectivityManager.activeNetwork
            val capabilities = currentNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }

            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                if (wifiInfo != null) {
                    val ssid = wifiInfo.ssid.removeSurrounding("\"")
                    val bssid = wifiInfo.bssid

                    val tracker = trackerRepository.findMatchingTracker(ssid, bssid)

                    if (tracker != null) {
                        eventDao.insert(
                            EventEntity(
                                trackerId = tracker.id,
                                eventType = "CONNECT",
                                timestamp = currentTime
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
