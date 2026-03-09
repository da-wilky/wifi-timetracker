package com.wifitracker.ui.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.repository.EventRepository
import com.wifitracker.data.repository.TrackerRepository
import com.wifitracker.domain.model.DateFilter
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.Tracker
import com.wifitracker.util.DateFilterCalculator
import com.wifitracker.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackersViewModel @Inject constructor(
    private val trackerRepository: TrackerRepository,
    private val eventRepository: EventRepository,
    private val eventDao: EventDao,
    private val localeManager: LocaleManager
) : ViewModel() {

    val showDays: StateFlow<Boolean> = localeManager.showDaysFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, localeManager.showDaysFlow.value)

    val trackers: StateFlow<List<Tracker>> = trackerRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    // Cache for tracker time calculation
    private data class TrackerTimeCache(
        val storedTime: Long,
        val lastConnectTimestamp: Long?
    )

    private val trackerTimeCache = mutableMapOf<Long, StateFlow<TrackerTimeCache>>()

    // Cache for the final display-time StateFlow so recompositions don't recreate it
    private val trackerDisplayTimeCache = mutableMapOf<Long, StateFlow<Long>>()

    fun getTrackerTime(trackerId: Long): StateFlow<Long> {
        return trackerDisplayTimeCache.getOrPut(trackerId) {
            // Get or create inner cache for this tracker
            val cacheFlow = trackerTimeCache.getOrPut(trackerId) {
                combine(
                    selectedFilter,
                    eventRepository.getEventsByTrackerFlow(trackerId)
                ) { filter, _ ->
                    // Only query DB when filter or events change
                    calculateStoredTimeAndLastConnect(trackerId, filter)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerTimeCache(0L, null))
            }

            // Combine cached data with timer for real-time updates
            combine(
                cacheFlow,
                flow { while(true) { emit(System.currentTimeMillis()); delay(1000) } }
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
                    lastConnect?.let {
                        totalTime += (event.timestamp - it)
                    }
                    lastConnect = null
                }
            }
        }

        return TrackerTimeCache(totalTime, lastConnect)
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    suspend fun refresh() {
        kotlinx.coroutines.delay(REFRESH_DISPLAY_DURATION_MS)
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

    companion object {
        private const val REFRESH_DISPLAY_DURATION_MS = 600L
    }
}
