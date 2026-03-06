package com.wifitracker.data.repository

import androidx.room.InvalidationTracker
import com.wifitracker.data.local.WifiTrackerDatabase
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.WifiEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val database: WifiTrackerDatabase
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

    fun getEventsByTrackerFlow(trackerId: Long): Flow<Unit> = channelFlow {
        // Emit initial value
        send(Unit)

        // Observe events table changes via Room's InvalidationTracker
        val observer = object : InvalidationTracker.Observer("events") {
            override fun onInvalidated(tables: Set<String>) {
                trySend(Unit)
            }
        }

        database.invalidationTracker.addObserver(observer)

        awaitClose {
            database.invalidationTracker.removeObserver(observer)
        }
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
