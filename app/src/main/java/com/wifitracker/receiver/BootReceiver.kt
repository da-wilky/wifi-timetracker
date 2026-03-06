package com.wifitracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.*
import com.wifitracker.data.local.WifiTrackerDatabase
import com.wifitracker.service.WifiTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if any trackers exist before starting service
            val result = goAsync()
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    val database = WifiTrackerDatabase.getInstance(context)
                    val trackerDao = database.trackerDao()

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
