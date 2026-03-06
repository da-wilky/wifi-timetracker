package com.wifitracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object {
        @Volatile
        private var INSTANCE: WifiTrackerDatabase? = null

        fun getInstance(context: Context): WifiTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiTrackerDatabase::class.java,
                    "wifi_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
