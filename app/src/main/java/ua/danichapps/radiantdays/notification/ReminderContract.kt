package ua.danichapps.radiantdays.notification

object ReminderContract {
    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_FIRE_TIME_MILLIS = "extra_fire_time_millis"

    const val ACTION_COMPLETE = "ua.danichapps.radiantdays.reminder.COMPLETE"
    const val ACTION_SNOOZE_5 = "ua.danichapps.radiantdays.reminder.SNOOZE_5"
    const val ACTION_SNOOZE_10 = "ua.danichapps.radiantdays.reminder.SNOOZE_10"

    const val WORK_DATA_EVENT_ID = "event_id"
    const val WORK_DATA_FIRE_TIME = "fire_time_millis"

    fun uniqueWorkName(eventId: Long): String = "note_reminder_$eventId"
}
