package app.swilk.wifitracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import app.swilk.wifitracker.data.local.entity.TrackerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Insert
    suspend fun insert(tracker: TrackerEntity): Long

    @Delete
    suspend fun delete(tracker: TrackerEntity)

    @Query("SELECT * FROM trackers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers WHERE id = :trackerId")
    fun getByTrackerIdFlow(trackerId: Long): Flow<TrackerEntity?>

    @Query("""
        SELECT * FROM trackers
        WHERE ssid = :ssid
        LIMIT 1
    """)
    suspend fun findMatchingTracker(ssid: String): TrackerEntity?

    @Query("SELECT * FROM trackers ORDER BY createdAt DESC")
    suspend fun getAllSnapshot(): List<TrackerEntity>
}
