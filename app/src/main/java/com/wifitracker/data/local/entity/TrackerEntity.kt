package com.wifitracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trackers",
    indices = [Index(value = ["ssid"])]
)
data class TrackerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val bssid: String?,
    val createdAt: Long
)
