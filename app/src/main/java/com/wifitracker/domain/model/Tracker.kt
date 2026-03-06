package com.wifitracker.domain.model

data class Tracker(
    val id: Long,
    val ssid: String,
    val bssid: String?,
    val createdAt: Long
)
