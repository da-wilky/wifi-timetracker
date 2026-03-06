package com.wifitracker.data.repository

import com.wifitracker.data.local.dao.BssidDao
import com.wifitracker.data.local.entity.BssidEntity
import com.wifitracker.domain.model.BssidRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BssidRepository @Inject constructor(
    private val bssidDao: BssidDao
) {
    suspend fun recordBssid(trackerId: Long, bssid: String, firstSeenAt: Long) {
        bssidDao.insertIfAbsent(
            BssidEntity(trackerId = trackerId, bssid = bssid, firstSeenAt = firstSeenAt)
        )
    }

    fun getByTrackerId(trackerId: Long): Flow<List<BssidRecord>> =
        bssidDao.getByTrackerId(trackerId).map { entities ->
            entities.map { BssidRecord(bssid = it.bssid, firstSeenAt = it.firstSeenAt) }
        }

    suspend fun getByTrackerIdSnapshot(trackerId: Long): List<BssidRecord> =
        bssidDao.getByTrackerIdSnapshot(trackerId).map {
            BssidRecord(bssid = it.bssid, firstSeenAt = it.firstSeenAt)
        }
}
