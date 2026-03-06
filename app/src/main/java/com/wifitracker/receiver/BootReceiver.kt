package com.wifitracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wifitracker.service.WifiTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, WifiTrackingService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
