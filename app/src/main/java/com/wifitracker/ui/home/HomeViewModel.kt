package com.wifitracker.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifitracker.data.local.dao.TrackerDao
import com.wifitracker.data.local.entity.TrackerEntity
import com.wifitracker.data.repository.TrackerRepository
import com.wifitracker.service.WifiMonitor
import com.wifitracker.service.WifiTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackerRepository: TrackerRepository,
    private val trackerDao: TrackerDao,
    wifiMonitor: WifiMonitor
) : ViewModel() {

    private val _wifiInfo = wifiMonitor.observeWifiNetwork()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentSsid: StateFlow<String?> = _wifiInfo.map { it?.ssid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentBssid: StateFlow<String?> = _wifiInfo.map { it?.bssid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTracked: StateFlow<Boolean> = combine(
        currentSsid,
        currentBssid,
        trackerRepository.getAll()
    ) { ssid, bssid, trackers ->
        if (ssid == null) return@combine false
        trackers.any { it.ssid == ssid && (it.bssid == null || it.bssid == bssid) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showOrphanedWarning = MutableStateFlow(
        context.getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_warning", false)
    )
    val showOrphanedWarning: StateFlow<Boolean> = _showOrphanedWarning.asStateFlow()

    private val _firstTrackerId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            trackerRepository.getAll().collect { trackers ->
                _firstTrackerId.value = trackers.firstOrNull()?.id
            }
        }
    }

    fun createTracker() {
        viewModelScope.launch {
            val ssid = currentSsid.value ?: return@launch
            val bssid = currentBssid.value

            trackerDao.insert(
                TrackerEntity(
                    ssid = ssid,
                    bssid = bssid,
                    createdAt = System.currentTimeMillis()
                )
            )

            // Start foreground service
            val serviceIntent = Intent(context, WifiTrackingService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun requestBatteryOptimization() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun dismissOrphanedWarning() {
        context.getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_warning", false)
            .apply()
        _showOrphanedWarning.value = false
    }

    fun getFirstTrackerId(): Long? = _firstTrackerId.value
}
