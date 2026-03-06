package com.wifitracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wifitracker.data.local.entity.BssidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BssidDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(bssid: BssidEntity): Long

    @Query("SELECT * FROM bssids WHERE trackerId = :trackerId ORDER BY firstSeenAt ASC")
    fun getByTrackerId(trackerId: Long): Flow<List<BssidEntity>>

    @Query("SELECT * FROM bssids WHERE trackerId = :trackerId ORDER BY firstSeenAt ASC")
    suspend fun getByTrackerIdSnapshot(trackerId: Long): List<BssidEntity>
}
