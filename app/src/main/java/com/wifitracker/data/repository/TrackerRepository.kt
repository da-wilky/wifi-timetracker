package com.wifitracker.data.repository

import com.wifitracker.data.local.dao.TrackerDao
import com.wifitracker.data.local.entity.TrackerEntity
import com.wifitracker.domain.model.Tracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepository @Inject constructor(
    private val trackerDao: TrackerDao
) {
    fun getAll(): Flow<List<Tracker>> = trackerDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(tracker: Tracker): Long {
        return trackerDao.insert(tracker.toEntity())
    }

    suspend fun delete(tracker: Tracker) {
        trackerDao.delete(tracker.toEntity())
    }

    suspend fun getAllSnapshot(): List<Tracker> {
        return trackerDao.getAllSnapshot().map { it.toDomain() }
    }

    suspend fun findMatchingTracker(ssid: String, bssid: String?): Tracker? {
        return trackerDao.findMatchingTracker(ssid, bssid)?.toDomain()
    }

    private fun TrackerEntity.toDomain() = Tracker(
        id = id,
        ssid = ssid,
        bssid = bssid,
        createdAt = createdAt
    )

    private fun Tracker.toEntity() = TrackerEntity(
        id = id,
        ssid = ssid,
        bssid = bssid,
        createdAt = createdAt
    )
}
