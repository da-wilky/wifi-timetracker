package com.wifitracker.domain.model

data class BssidRecord(
    val bssid: String,
    val firstSeenAt: Long
)
