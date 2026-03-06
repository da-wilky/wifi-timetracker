package com.wifitracker.data.repository

import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.WifiEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
) {
    suspend fun insert(event: WifiEvent): Long {
        return eventDao.insert(event.toEntity())
    }

    suspend fun getEventsByTrackerAndDateRange(
        trackerId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<WifiEvent> {
        return eventDao.getEventsByTrackerAndDateRange(trackerId, startTimestamp, endTimestamp)
            .map { it.toDomain() }
    }

    suspend fun updateEvent(eventId: Long, newTimestamp: Long) {
        eventDao.updateEvent(eventId, newTimestamp)
    }

    suspend fun deleteAllByTracker(trackerId: Long) {
        eventDao.deleteAllByTracker(trackerId)
    }

    private fun EventEntity.toDomain() = WifiEvent(
        id = id,
        trackerId = trackerId,
        eventType = EventType.valueOf(eventType),
        timestamp = timestamp,
        isEditable = false // Will be computed in ViewModel based on pairing logic
    )

    private fun WifiEvent.toEntity() = EventEntity(
        id = id,
        trackerId = trackerId,
        eventType = eventType.name,
        timestamp = timestamp
    )
}
