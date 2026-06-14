package ua.danichapps.radiantdays.notification

import android.content.Context

/** Tracks which reminder fire time was already shown per note id. */
class ReminderNotifierStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasShown(eventId: Long, fireTimeMillis: Long): Boolean =
        prefs.getLong(key(eventId), Long.MIN_VALUE) == fireTimeMillis

    fun markShown(eventId: Long, fireTimeMillis: Long) {
        prefs.edit().putLong(key(eventId), fireTimeMillis).apply()
    }

    fun clear(eventId: Long) {
        prefs.edit().remove(key(eventId)).apply()
    }

    private fun key(eventId: Long) = "shown_fire_$eventId"

    private companion object {
        const val PREFS_NAME = "reminder_notifier"
    }
}
