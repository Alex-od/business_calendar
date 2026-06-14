package ua.danichapps.radiantdays.domain.model

/**
 * Rules for when a note should fire a local reminder notification.
 *
 * Effective fire time = [CalendarEvent.alarmTimeMillis] minus [CalendarEvent.notificationMinutesBefore].
 */
object ReminderPolicy {

    private const val MILLIS_PER_MINUTE = 60_000L

    fun reminderFireTimeMillis(event: CalendarEvent): Long? {
        val alarmMillis = event.alarmTimeMillis ?: return null
        val offsetMillis = event.notificationMinutesBefore.coerceAtLeast(0) * MILLIS_PER_MINUTE
        return alarmMillis - offsetMillis
    }

    fun shouldScheduleReminder(
        event: CalendarEvent,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (event.alarmTimeMillis == null || event.isCompleted) return false
        val fireMillis = reminderFireTimeMillis(event) ?: return false
        return fireMillis > nowMillis - MILLIS_PER_MINUTE
    }
}
