package ua.danichapps.radiantdays.ui.addevent

import java.util.Calendar

internal val REMINDER_OFFSET_MINUTES_OPTIONS = listOf(0, 5, 15, 30)

internal fun millisPlusMinutes(fromMillis: Long, minutes: Int): Long =
    fromMillis + minutes * 60_000L

internal fun millisPlusHours(fromMillis: Long, hours: Int): Long =
    fromMillis + hours * 3_600_000L

internal fun tomorrowAtNineMillis(): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun noteDayAtNineMillis(startTimeMillis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startTimeMillis
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun reminderFireTimeMillis(alarmTimeMillis: Long, notificationMinutesBefore: Int): Long =
    alarmTimeMillis - notificationMinutesBefore.coerceAtLeast(0) * 60_000L
