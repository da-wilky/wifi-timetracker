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
import com.wifitracker.domain.model.WifiEvent
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val trackerId: Long = savedStateHandle.get<Long>("trackerId") ?: 0L

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

    val bssidRecords: StateFlow<List<BssidRecord>> = bssidRepository
        .getByTrackerId(trackerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecentSessions()
    }

    private fun loadRecentSessions() {
        viewModelScope.launch {
            val events = eventDao.getRecentTwoSessions(trackerId)
            _recentSessions.value = events.map { entity ->
                WifiEvent(
                    id = entity.id,
                    trackerId = entity.trackerId,
                    eventType = com.wifitracker.domain.model.EventType.valueOf(entity.eventType),
                    timestamp = entity.timestamp,
                    isEditable = false
                )
            }
        }
    }

    fun updateEvent(eventId: Long, newTimestamp: Long) {
        viewModelScope.launch {
            eventDao.updateEvent(eventId, newTimestamp)
            loadRecentSessions()
            pagingSource?.invalidate()
        }
    }
}
