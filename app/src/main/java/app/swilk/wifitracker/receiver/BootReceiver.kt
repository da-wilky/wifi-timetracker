package app.swilk.wifitracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.swilk.wifitracker.data.local.dao.TrackerDao
import app.swilk.wifitracker.service.WifiTrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var trackerDao: TrackerDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val result = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val hasTrackers = trackerDao.getAllSnapshot().isNotEmpty()
                    if (hasTrackers) {
                        val serviceIntent = Intent(context, WifiTrackingService::class.java)
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                    result.finish()
                } catch (e: Exception) {
                    result.finish()
                }
            }
        }
    }
}
