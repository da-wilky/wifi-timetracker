package app.swilk.wifitracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatDuration without days shows total hours`() {
        // 2 days + 3 hours + 45 minutes + 10 seconds = 51 hours total
        val millis = ((2 * 24 + 3) * 3600L + 45 * 60L + 10L) * 1000L
        val result = TimeFormatter.formatDuration(millis, showDays = false)
        assertEquals("51h 45m 10s", result)
    }

    @Test
    fun `formatDuration with days extracts days separately`() {
        // 2 days + 3 hours + 45 minutes + 10 seconds
        val millis = ((2 * 24 + 3) * 3600L + 45 * 60L + 10L) * 1000L
        val result = TimeFormatter.formatDuration(millis, showDays = true)
        assertEquals("2d 3h 45m 10s", result)
    }

    @Test
    fun `formatDuration default is without days`() {
        val millis = (25 * 3600L + 30 * 60L + 5L) * 1000L
        val resultDefault = TimeFormatter.formatDuration(millis)
        val resultExplicit = TimeFormatter.formatDuration(millis, showDays = false)
        assertEquals(resultExplicit, resultDefault)
    }

    @Test
    fun `formatDuration with days and zero days shows no days prefix`() {
        // Less than 1 day: 3 hours + 5 minutes + 20 seconds
        val millis = (3 * 3600L + 5 * 60L + 20L) * 1000L
        val result = TimeFormatter.formatDuration(millis, showDays = true)
        assertEquals("3h 5m 20s", result)
    }

    @Test
    fun `formatDuration without days less than one day shows correct hours`() {
        // 3 hours + 5 minutes + 20 seconds
        val millis = (3 * 3600L + 5 * 60L + 20L) * 1000L
        val result = TimeFormatter.formatDuration(millis, showDays = false)
        assertEquals("3h 5m 20s", result)
    }

    @Test
    fun `formatDuration handles zero millis`() {
        assertEquals("0h 0m 0s", TimeFormatter.formatDuration(0L, showDays = false))
        assertEquals("0h 0m 0s", TimeFormatter.formatDuration(0L, showDays = true))
    }
}
