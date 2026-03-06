package com.wifitracker.domain.model

data class WifiEvent(
    val id: Long,
    val trackerId: Long,
    val eventType: EventType,
    val timestamp: Long,
    val isEditable: Boolean,
    val minTimestamp: Long? = null,
    val maxTimestamp: Long? = null
)
