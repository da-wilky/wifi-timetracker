package app.swilk.wifitracker.domain.model

sealed class DateFilter {
    data object All : DateFilter()
    data object Today : DateFilter()
    data object Yesterday : DateFilter()
    data object ThisWeek : DateFilter()
    data object LastWeek : DateFilter()
    data object ThisMonth : DateFilter()
    data object LastMonth : DateFilter()
    data class Custom(val startMs: Long, val endMs: Long) : DateFilter()
}
