package com.wifitracker.ui.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.repository.EventRepository
import com.wifitracker.data.repository.TrackerRepository
import com.wifitracker.domain.model.DateFilter
import com.wifitracker.domain.model.Tracker
import com.wifitracker.util.DateFilterCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackersViewModel @Inject constructor(
    private val trackerRepository: TrackerRepository,
    private val eventRepository: EventRepository,
    private val eventDao: EventDao
) : ViewModel() {

    val trackers: StateFlow<List<Tracker>> = trackerRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    fun getTrackerTime(trackerId: Long): StateFlow<Long> {
        return combine(
            selectedFilter,
            flow { while(true) { emit(System.currentTimeMillis()); delay(1000) } }
        ) { filter, _ ->
            calculateTrackerTime(trackerId, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    }

    private suspend fun calculateTrackerTime(trackerId: Long, filter: DateFilter): Long {
        val (start, end) = DateFilterCalculator.calculateRange(filter)
        val events = eventRepository.getEventsByTrackerAndDateRange(trackerId, start, end)
            .sortedBy { it.timestamp }

        var totalTime = 0L
        var lastConnect: Long? = null

        for (event in events) {
            when (event.eventType.name) {
                "CONNECT" -> lastConnect = event.timestamp
                "DISCONNECT" -> {
                    lastConnect?.let {
                        totalTime += (event.timestamp - it)
                    }
                    lastConnect = null
                }
            }
        }

        // Add ongoing session time
        lastConnect?.let {
            totalTime += (System.currentTimeMillis() - it)
        }

        return totalTime
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
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
}
