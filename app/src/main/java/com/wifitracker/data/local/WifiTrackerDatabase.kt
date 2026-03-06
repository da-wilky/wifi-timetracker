package com.wifitracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.data.local.dao.TrackerDao
import com.wifitracker.data.local.entity.EventEntity
import com.wifitracker.data.local.entity.TrackerEntity

@Database(
    entities = [TrackerEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WifiTrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun eventDao(): EventDao
}
