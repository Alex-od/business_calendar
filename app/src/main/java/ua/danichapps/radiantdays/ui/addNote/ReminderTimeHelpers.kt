package ua.danichapps.radiantdays.ui.addNote

import java.util.Calendar

/** Preset options for "minutes before" notification offset. */
internal val REMINDER_OFFSET_MINUTES_OPTIONS = listOf(0, 5, 15, 30)

/** Applies a new date while keeping the time from [originalMillis]. */
internal fun mergeDateIntoMillis(dateMillis: Long, originalMillis: Long): Long {
    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val originalCal = Calendar.getInstance().apply { timeInMillis = originalMillis }
    dateCal.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
    dateCal.set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
    dateCal.set(Calendar.SECOND, 0)
    dateCal.set(Calendar.MILLISECOND, 0)
    return dateCal.timeInMillis
}

/** Applies [hour] and [minute] while keeping the date from [originalMillis]. */
internal fun mergeTimeIntoMillis(originalMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = originalMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
