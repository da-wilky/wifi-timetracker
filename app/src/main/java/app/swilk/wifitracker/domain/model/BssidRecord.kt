package app.swilk.wifitracker.domain.model

data class BssidRecord(
    val bssid: String,
    val firstSeenAt: Long
)
