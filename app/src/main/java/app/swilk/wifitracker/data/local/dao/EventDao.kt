package app.swilk.wifitracker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.swilk.wifitracker.data.local.entity.EventEntity

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun delete(eventId: Long)

    @Query("""
        SELECT * FROM events
        WHERE trackerId = :trackerId
        ORDER BY timestamp DESC
        LIMIT 4
    """)
    suspend fun getRecentTwoSessions(trackerId: Long): List<EventEntity>

    @Query("""
        SELECT * FROM events
        WHERE trackerId = :trackerId
        ORDER BY timestamp DESC
    """)
    fun getEventsPaged(trackerId: Long): PagingSource<Int, EventEntity>

    @Query("""
        SELECT * FROM events
        WHERE trackerId = :trackerId
        AND timestamp >= :startTimestamp
        AND timestamp <= :endTimestamp
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsByTrackerAndDateRange(
        trackerId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<EventEntity>

    @Query("""
        SELECT e1.* FROM events e1
        WHERE e1.eventType = 'CONNECT'
        AND (
            NOT EXISTS (
                SELECT 1 FROM events e2
                WHERE e2.trackerId = e1.trackerId
                AND e2.timestamp > e1.timestamp
            )
            OR
            (
                SELECT e2.eventType FROM events e2
                WHERE e2.trackerId = e1.trackerId
                AND e2.timestamp > e1.timestamp
                ORDER BY e2.timestamp ASC
                LIMIT 1
            ) = 'CONNECT'
        )
    """)
    suspend fun findOpenConnects(): List<EventEntity>

    @Query("UPDATE events SET timestamp = :newTimestamp WHERE id = :eventId")
    suspend fun updateEvent(eventId: Long, newTimestamp: Long)

    @Query("DELETE FROM events WHERE trackerId = :trackerId")
    suspend fun deleteAllByTracker(trackerId: Long)
}
