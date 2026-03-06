package com.wifitracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId", "timestamp"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: Long,
    val eventType: String, // "CONNECT" or "DISCONNECT"
    val timestamp: Long
)
