package com.wifitracker.util

import com.wifitracker.domain.model.DateFilter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object DateFilterCalculator {
    fun calculateRange(filter: DateFilter, now: Instant = Instant.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val zonedNow = now.atZone(zone)

        return when (filter) {
            is DateFilter.All -> Pair(0L, Long.MAX_VALUE)
            is DateFilter.Today -> {
                val startOfDay = zonedNow.truncatedTo(ChronoUnit.DAYS)
                Pair(startOfDay.toInstant().toEpochMilli(), now.toEpochMilli())
            }
            is DateFilter.Yesterday -> {
                val yesterday = zonedNow.minusDays(1)
                val startOfYesterday = yesterday.truncatedTo(ChronoUnit.DAYS)
                val endOfYesterday = startOfYesterday.plusDays(1).minusNanos(1)
                Pair(startOfYesterday.toInstant().toEpochMilli(), endOfYesterday.toInstant().toEpochMilli())
            }
            is DateFilter.ThisWeek -> {
                val startOfWeek = zonedNow.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS)
                Pair(startOfWeek.toInstant().toEpochMilli(), now.toEpochMilli())
            }
            is DateFilter.LastWeek -> {
                val lastWeekStart = zonedNow.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .minusWeeks(1)
                    .truncatedTo(ChronoUnit.DAYS)
                val lastWeekEnd = lastWeekStart.plusWeeks(1).minusNanos(1)
                Pair(lastWeekStart.toInstant().toEpochMilli(), lastWeekEnd.toInstant().toEpochMilli())
            }
            is DateFilter.ThisMonth -> {
                val startOfMonth = zonedNow.with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS)
                Pair(startOfMonth.toInstant().toEpochMilli(), now.toEpochMilli())
            }
            is DateFilter.LastMonth -> {
                val lastMonthStart = zonedNow.minusMonths(1)
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS)
                val lastMonthEnd = lastMonthStart.with(TemporalAdjusters.lastDayOfMonth())
                    .plusDays(1)
                    .minusNanos(1)
                Pair(lastMonthStart.toInstant().toEpochMilli(), lastMonthEnd.toInstant().toEpochMilli())
            }
            is DateFilter.Custom -> Pair(filter.startMs, filter.endMs)
        }
    }
}
