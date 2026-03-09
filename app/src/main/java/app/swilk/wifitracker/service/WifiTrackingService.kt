package app.swilk.wifitracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.swilk.wifitracker.R
import app.swilk.wifitracker.data.local.dao.EventDao
import app.swilk.wifitracker.data.local.dao.BssidDao
import app.swilk.wifitracker.data.local.entity.BssidEntity
import app.swilk.wifitracker.data.local.entity.EventEntity
import app.swilk.wifitracker.data.repository.TrackerRepository
import app.swilk.wifitracker.ui.MainActivity
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

    @Inject
    lateinit var bssidDao: BssidDao

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
        // On API 34+ the foreground service type must be passed to startForeground();
        // omitting it throws MissingForegroundServiceTypeException and crashes the service.
        startForeground(
            NOTIFICATION_ID,
            createNotification(null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        serviceScope.launch {
            wifiMonitor.observeWifiNetwork().collect { state ->
                networkChangeMutex.withLock {
                    handleNetworkChange(state)
                }
            }
        }
    }

    private suspend fun handleNetworkChange(state: WifiNetworkState) {
        when (state) {
            is WifiNetworkState.SsidUnavailable -> {
                // Location Services are off – the device may still be connected to the same
                // network.  Do not record a spurious DISCONNECT; just return.
                return
            }

            is WifiNetworkState.Disconnected -> {
                val trackerId = currentTrackerId
                if (currentSsid != null && trackerId != null) {
                    eventDao.insert(
                        EventEntity(
                            trackerId = trackerId,
                            eventType = "DISCONNECT",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                currentSsid = null
                currentBssid = null
                currentTrackerId = null
                updateNotification(null)
            }

            is WifiNetworkState.Connected -> {
                val newSsid = state.ssid
                val newBssid = state.bssid

                if (newSsid != currentSsid) {
                    // SSID changed – record disconnect from previous network if applicable
                    val prevTrackerId = currentTrackerId
                    if (currentSsid != null && prevTrackerId != null) {
                        eventDao.insert(
                            EventEntity(
                                trackerId = prevTrackerId,
                                eventType = "DISCONNECT",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    currentSsid = newSsid
                    currentBssid = newBssid
                    currentTrackerId = null

                    val tracker = trackerRepository.findMatchingTracker(newSsid)
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

                    updateNotification(newSsid)
                } else if (newBssid != currentBssid) {
                    // Only the BSSID changed (roamed to another access point on the same network).
                    // Do NOT record a disconnect/connect – just update the current BSSID.
                    currentBssid = newBssid
                }

                // Record the BSSID in the bssids table if we are actively tracking this SSID
                val trackerId = currentTrackerId
                if (trackerId != null && newBssid != null) {
                    bssidDao.insertIfAbsent(
                        BssidEntity(
                            trackerId = trackerId,
                            bssid = newBssid,
                            firstSeenAt = System.currentTimeMillis()
                        )
                    )
                }
            }
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

        // Tapping the notification opens the app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Stop" action opens the app and shows a shutdown confirmation dialog
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SHOW_STOP_DIALOG, true)
        }
        val stopPendingIntent = PendingIntent.getActivity(
            this, REQUEST_CODE_STOP, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            // Prevent the user from dismissing the foreground-service notification
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop_action),
                stopPendingIntent
            )
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
        private const val REQUEST_CODE_STOP = 100
    }
}
