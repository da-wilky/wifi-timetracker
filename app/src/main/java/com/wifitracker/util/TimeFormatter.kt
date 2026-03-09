package com.wifitracker.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatter {
    fun formatDuration(millis: Long, showDays: Boolean = false): String {
        val totalSeconds = millis / 1000
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (showDays) {
            val days = totalSeconds / 86400
            val hours = (totalSeconds % 86400) / 3600
            buildString {
                if (days > 0) append("${days}d ")
                append("${hours}h ")
                append("${minutes}m ")
                append("${seconds}s")
            }.trim()
        } else {
            val hours = totalSeconds / 3600
            "${hours}h ${minutes}m ${seconds}s"
        }
    }

    private val dateTimeFormatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    fun formatDateTime(millis: Long): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(millis))

    fun formatDateRange(startMs: Long, endMs: Long): String =
        "${formatDateTime(startMs)} – ${formatDateTime(endMs)}"
}
