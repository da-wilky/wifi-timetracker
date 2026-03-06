package com.wifitracker.util

object TimeFormatter {
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (days > 0) append("${days}d ")
            append("${hours}h ")
            append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}
