package ua.danichapps.radiantdays.calendar

import java.util.Calendar

/** Day-of-week header labels. Sunday-first to match [Calendar.DAY_OF_WEEK]. */
val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

sealed interface CalendarDay {
    data object Empty : CalendarDay
    data class Day(val number: Int, val millis: Long) : CalendarDay
}

fun buildMonthDays(monthMillis: Long): List<CalendarDay> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    cal.set(year, month, 1)
    val firstWeekDay = cal.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<CalendarDay>()
    repeat(firstWeekDay - 1) { days += CalendarDay.Empty }
    for (day in 1..daysInMonth) {
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        days += CalendarDay.Day(day, cal.timeInMillis)
    }
    return days
}

fun sameDay(refCal: Calendar, millis: Long): Boolean {
    val other = Calendar.getInstance().apply { timeInMillis = millis }
    return refCal.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        refCal.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

/** Midnight of the given epoch millis in the device's local timezone. */
fun normaliseToDayStart(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun dayWindow(millis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return start to cal.timeInMillis
}

fun monthWindow(monthMillis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    cal.set(year, month, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    return start to cal.timeInMillis
}
