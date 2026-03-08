package com.wifitracker.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.dao.TrackerDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.data.local.entity.TrackerEntity
import com.wifitracker.data.repository.EventRepository
import com.wifitracker.data.repository.TrackerRepository
import com.wifitracker.domain.model.DateFilter
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.Tracker
import com.wifitracker.service.WifiMonitor
import com.wifitracker.service.WifiNetworkState
import com.wifitracker.service.WifiTrackingService
import com.wifitracker.util.DateFilterCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackerRepository: TrackerRepository,
    private val trackerDao: TrackerDao,
    private val eventRepository: EventRepository,
    private val eventDao: EventDao,
    private val wifiMonitor: WifiMonitor
) : ViewModel() {

    // Use MutableStateFlow so we can directly update the state when refresh() is called.
    // This mirrors what happens on app restart, where getCurrentState() provides the initial value.
    private val _wifiState = MutableStateFlow<WifiNetworkState>(WifiNetworkState.Disconnected)

    init {
        viewModelScope.launch {
            // Get initial state
            _wifiState.value = wifiMonitor.getCurrentState()

            // Then collect from the network callback and update our state
            wifiMonitor.observeWifiNetwork().collect { state ->
                _wifiState.value = state
            }
        }
    }

    val currentSsid: StateFlow<String?> = _wifiState.map {
        (it as? WifiNetworkState.Connected)?.ssid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentBssid: StateFlow<String?> = _wifiState.map {
        (it as? WifiNetworkState.Connected)?.bssid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trackers: StateFlow<List<Tracker>> = trackerRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isTracked: StateFlow<Boolean> = combine(
        currentSsid,
        trackers
    ) { ssid, trackerList ->
        if (ssid == null) return@combine false
        trackerList.any { it.ssid == ssid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _showOrphanedWarning = MutableStateFlow(
        context.getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_warning", false)
    )
    val showOrphanedWarning: StateFlow<Boolean> = _showOrphanedWarning.asStateFlow()

    private val _firstTrackerId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            trackers.collect { list ->
                _firstTrackerId.value = list.firstOrNull()?.id
            }
        }
    }

    // ----- Timer calculation (mirrors TrackersViewModel) -----

    private data class TrackerTimeCache(
        val storedTime: Long,
        val lastConnectTimestamp: Long?
    )

    private val trackerTimeCache = mutableMapOf<Long, StateFlow<TrackerTimeCache>>()
    private val trackerDisplayTimeCache = mutableMapOf<Long, StateFlow<Long>>()

    fun getTrackerTime(trackerId: Long): StateFlow<Long> {
        return trackerDisplayTimeCache.getOrPut(trackerId) {
            val cacheFlow = trackerTimeCache.getOrPut(trackerId) {
                combine(
                    selectedFilter,
                    eventRepository.getEventsByTrackerFlow(trackerId)
                ) { filter, _ ->
                    calculateStoredTimeAndLastConnect(trackerId, filter)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerTimeCache(0L, null))
            }

            combine(
                cacheFlow,
                flow { while (true) { emit(System.currentTimeMillis()); delay(1000) } }
            ) { cache, currentTime ->
                cache.lastConnectTimestamp?.let { lastConnect ->
                    cache.storedTime + (currentTime - lastConnect)
                } ?: cache.storedTime
            }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
        }
    }

    private suspend fun calculateStoredTimeAndLastConnect(
        trackerId: Long,
        filter: DateFilter
    ): TrackerTimeCache {
        val (start, end) = DateFilterCalculator.calculateRange(filter)
        val events = eventRepository.getEventsByTrackerAndDateRange(trackerId, start, end)
            .sortedBy { it.timestamp }

        var totalTime = 0L
        var lastConnect: Long? = null

        for (event in events) {
            when (event.eventType) {
                EventType.CONNECT -> lastConnect = event.timestamp
                EventType.DISCONNECT -> {
                    lastConnect?.let { totalTime += (event.timestamp - it) }
                    lastConnect = null
                }
            }
        }

        return TrackerTimeCache(totalTime, lastConnect)
    }

    // ----- Actions -----

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    suspend fun refresh() {
        // Re-query the current WiFi state and update our MutableStateFlow directly.
        // This is exactly what happens on app restart (getCurrentState() provides the value),
        // so it should behave identically without needing to restart the app.
        _wifiState.value = wifiMonitor.getCurrentState()
        kotlinx.coroutines.delay(REFRESH_DISPLAY_DURATION_MS)
    }

    companion object {
        private const val REFRESH_DISPLAY_DURATION_MS = 600L
    }

    fun createTracker() {
        viewModelScope.launch {
            val ssid = currentSsid.value ?: return@launch
            val now = System.currentTimeMillis()

            val trackerId = trackerDao.insert(
                TrackerEntity(
                    ssid = ssid,
                    bssid = null,
                    createdAt = now
                )
            )

            // If the background service is already running it will NOT detect the
            // newly-created tracker because the SSID has not changed from its
            // perspective. Insert a synthetic CONNECT event so the timer starts
            // immediately without waiting for the next network transition.
            val isServiceRunning = context
                .getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
                .getBoolean("service_running", false)
            if (isServiceRunning) {
                eventDao.insert(
                    EventEntity(
                        trackerId = trackerId,
                        eventType = EventType.CONNECT.name,
                        timestamp = now
                    )
                )
            }

            val serviceIntent = Intent(context, WifiTrackingService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun deleteTracker(tracker: Tracker) {
        viewModelScope.launch {
            trackerRepository.delete(tracker)
        }
    }

    fun resetTimer(trackerId: Long) {
        viewModelScope.launch {
            eventDao.deleteAllByTracker(trackerId)
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
