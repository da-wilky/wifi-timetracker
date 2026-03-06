package com.wifitracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wifitracker.R
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.data.repository.TrackerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class WifiTrackingService : Service() {

    @Inject
    lateinit var wifiMonitor: WifiMonitor

    @Inject
    lateinit var trackerRepository: TrackerRepository

    @Inject
    lateinit var eventDao: EventDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSsid: String? = null
    private var currentBssid: String? = null
    private var currentTrackerId: Long? = null
    private val networkChangeMutex = Mutex()

    override fun onCreate() {
        super.onCreate()

        // Mark service as running
        getSharedPreferences("wifi_tracker_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("service_running", true)
            .apply()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(null))

        serviceScope.launch {
            wifiMonitor.observeWifiNetwork().collect { networkInfo ->
                networkChangeMutex.withLock {
                    handleNetworkChange(networkInfo)
                }
            }
        }
    }

    private suspend fun handleNetworkChange(networkInfo: WifiNetworkInfo?) {
        val newSsid = networkInfo?.ssid
        val newBssid = networkInfo?.bssid

        if (newSsid != currentSsid || newBssid != currentBssid) {
            // Disconnect event
            if (currentSsid != null && currentTrackerId != null) {
                eventDao.insert(
                    EventEntity(
                        trackerId = currentTrackerId!!,
                        eventType = "DISCONNECT",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            // Connect event
            currentSsid = newSsid
            currentBssid = newBssid
            currentTrackerId = null

            if (newSsid != null) {
                val tracker = trackerRepository.findMatchingTracker(newSsid, newBssid)

                if (tracker != null) {
                    currentTrackerId = tracker.id
                    eventDao.insert(
                        EventEntity(
                            trackerId = tracker.id,
                            eventType = "CONNECT",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            updateNotification(newSsid)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(ssid: String?): Notification {
        val text = if (ssid != null && currentTrackerId != null) {
            getString(R.string.currently_tracking, ssid)
        } else {
            getString(R.string.not_tracking)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(ssid: String?) {
        val notification = createNotification(ssid)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY to ensure Android restarts the service after process death
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        // Mark service as stopped
        getSharedPreferences("wifi_tracker_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("service_running", false)
            .apply()

        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "wifi_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
