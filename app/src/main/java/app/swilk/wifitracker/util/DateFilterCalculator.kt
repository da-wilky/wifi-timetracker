package app.swilk.wifitracker.util

import app.swilk.wifitracker.domain.model.DateFilter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object DateFilterCalculator {
    /**
     * Calculates the [start, end) millisecond range for the given [filter].
     *
     * All preset ranges use clean midnight boundaries (no ±1 nanosecond adjustments),
     * so that boundary time-calculation rules (partial sessions that overlap a range
     * boundary) can be applied consistently.
     *
     *  - Today      → 00:00 today … 00:00 tomorrow
     *  - Yesterday  → 00:00 yesterday … 00:00 today
     *  - ThisWeek   → 00:00 Monday of the current week … 00:00 next Monday
     *  - LastWeek   → 00:00 last Monday … 00:00 this Monday
     *  - ThisMonth  → 00:00 first day of this month … 00:00 first day of next month
     *  - LastMonth  → 00:00 first day of last month … 00:00 first day of this month
     *  - Custom     → the explicit start/end timestamps provided by the user
     *  - All        → 0 … Long.MAX_VALUE
     */
    fun calculateRange(filter: DateFilter, now: Instant = Instant.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val zonedNow = now.atZone(zone)

        return when (filter) {
            is DateFilter.All -> Pair(0L, Long.MAX_VALUE)
            is DateFilter.Today -> {
                val startOfDay = zonedNow.truncatedTo(ChronoUnit.DAYS)
                val startOfNextDay = startOfDay.plusDays(1)
                Pair(startOfDay.toInstant().toEpochMilli(), startOfNextDay.toInstant().toEpochMilli())
            }
            is DateFilter.Yesterday -> {
                val startOfYesterday = zonedNow.minusDays(1).truncatedTo(ChronoUnit.DAYS)
                val startOfToday = startOfYesterday.plusDays(1)
                Pair(startOfYesterday.toInstant().toEpochMilli(), startOfToday.toInstant().toEpochMilli())
            }
            is DateFilter.ThisWeek -> {
                val startOfWeek = zonedNow
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS)
                val startOfNextWeek = startOfWeek.plusWeeks(1)
                Pair(startOfWeek.toInstant().toEpochMilli(), startOfNextWeek.toInstant().toEpochMilli())
            }
            is DateFilter.LastWeek -> {
                val startOfLastWeek = zonedNow
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .minusWeeks(1)
                    .truncatedTo(ChronoUnit.DAYS)
                val startOfThisWeek = startOfLastWeek.plusWeeks(1)
                Pair(startOfLastWeek.toInstant().toEpochMilli(), startOfThisWeek.toInstant().toEpochMilli())
            }
            is DateFilter.ThisMonth -> {
                val startOfMonth = zonedNow
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS)
                val startOfNextMonth = startOfMonth.with(TemporalAdjusters.firstDayOfNextMonth())
                Pair(startOfMonth.toInstant().toEpochMilli(), startOfNextMonth.toInstant().toEpochMilli())
            }
            is DateFilter.LastMonth -> {
                val startOfLastMonth = zonedNow.minusMonths(1)
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS)
                val startOfThisMonth = startOfLastMonth.with(TemporalAdjusters.firstDayOfNextMonth())
                Pair(startOfLastMonth.toInstant().toEpochMilli(), startOfThisMonth.toInstant().toEpochMilli())
            }
            is DateFilter.Custom -> Pair(filter.startMs, filter.endMs)
        }
    }
}
