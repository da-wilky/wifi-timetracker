package com.wifitracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.repository.BssidRepository
import com.wifitracker.domain.model.BssidRecord
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.WifiEvent
import com.wifitracker.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventLogViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val bssidRepository: BssidRepository,
    private val localeManager: LocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val showDays: StateFlow<Boolean> = localeManager.showDaysFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, localeManager.showDaysFlow.value)

    private val trackerId: Long = savedStateHandle.get<Long>("trackerId") ?: 0L

    /** -1L when no filter is active (i.e. user navigated without a time filter). */
    val filterStart: Long = savedStateHandle.get<Long>("filterStart") ?: -1L
    val filterEnd: Long = savedStateHandle.get<Long>("filterEnd") ?: -1L

    /** True when the user opened this screen from a filtered tracker list. */
    val hasFilter: Boolean = filterStart != -1L && filterEnd != -1L

    private var pagingSource: EventLogPagingSource? = null

    val eventsPager: Flow<PagingData<WifiEvent>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false
        )
    ) {
        EventLogPagingSource(eventDao, trackerId).also { pagingSource = it }
    }.flow.cachedIn(viewModelScope)

    private val _recentSessions = MutableStateFlow<List<WifiEvent>>(emptyList())
    val recentSessions: StateFlow<List<WifiEvent>> = _recentSessions

    private val _filteredSessions = MutableStateFlow<List<WifiEvent>>(emptyList())
    val filteredSessions: StateFlow<List<WifiEvent>> = _filteredSessions

    private val _filteredTime = MutableStateFlow(0L)
    val filteredTime: StateFlow<Long> = _filteredTime

    val bssidRecords: StateFlow<List<BssidRecord>> = bssidRepository
        .getByTrackerId(trackerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecentSessions()
        if (hasFilter) {
            loadFilteredSessions()
        }
    }

    private fun loadRecentSessions() {
        viewModelScope.launch {
            val events = eventDao.getRecentTwoSessions(trackerId)
            _recentSessions.value = events.map { entity ->
                WifiEvent(
                    id = entity.id,
                    trackerId = entity.trackerId,
                    eventType = EventType.valueOf(entity.eventType),
                    timestamp = entity.timestamp,
                    isEditable = false
                )
            }
        }
    }

    private fun loadFilteredSessions() {
        viewModelScope.launch {
            val entities = eventDao.getEventsByTrackerAndDateRange(trackerId, filterStart, filterEnd)
            val events = entities.map { entity ->
                WifiEvent(
                    id = entity.id,
                    trackerId = entity.trackerId,
                    eventType = EventType.valueOf(entity.eventType),
                    timestamp = entity.timestamp,
                    isEditable = false
                )
            }
            _filteredSessions.value = events
            _filteredTime.value = calculateFilteredTime(events, filterStart, filterEnd)
        }
    }

    /**
     * Calculates total connected time for the events within the filter range, applying
     * boundary rules:
     * - If the first event in the range is a DISCONNECT, count from [rangeStart] to that event.
     * - If the last event in the range is a CONNECT, count from that event to
     *   min([rangeEnd], now) so future ranges are capped at the current time.
     */
    private fun calculateFilteredTime(
        events: List<WifiEvent>,
        rangeStart: Long,
        rangeEnd: Long
    ): Long {
        val sortedEvents = events.sortedBy { it.timestamp }
        var totalTime = 0L
        var lastConnect: Long? = null
        var isFirstEvent = true

        for (event in sortedEvents) {
            when (event.eventType) {
                EventType.CONNECT -> lastConnect = event.timestamp
                EventType.DISCONNECT -> {
                    if (lastConnect != null) {
                        totalTime += event.timestamp - lastConnect
                    } else if (isFirstEvent) {
                        // First event in range is a DISCONNECT: session started before range began.
                        totalTime += event.timestamp - rangeStart
                    }
                    lastConnect = null
                }
            }
            isFirstEvent = false
        }

        // Last event is a CONNECT: count to the earlier of rangeEnd and now.
        lastConnect?.let {
            val effectiveEnd = minOf(rangeEnd, System.currentTimeMillis())
            totalTime += effectiveEnd - it
        }

        return totalTime
    }

    fun updateEvent(eventId: Long, newTimestamp: Long) {
        viewModelScope.launch {
            eventDao.updateEvent(eventId, newTimestamp)
            loadRecentSessions()
            if (hasFilter) loadFilteredSessions()
            pagingSource?.invalidate()
        }
    }
}
