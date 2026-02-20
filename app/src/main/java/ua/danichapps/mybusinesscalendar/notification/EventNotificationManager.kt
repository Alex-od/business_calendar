package ua.danichapps.mybusinesscalendar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ua.danichapps.mybusinesscalendar.MainActivity
import ua.danichapps.mybusinesscalendar.R

/**
 * Manages notification channel creation and shows event reminder notifications.
 *
 * Designed as a injectable singleton (via Koin) so it can be used from both
 * [EventNotificationWorker] and the Application class.
 *
 * @param context Application context (provided by Koin's `androidContext()`).
 */
class EventNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID   = "event_reminders"
        const val CHANNEL_NAME = "Event Reminders"
    }

    /**
     * Creates the notification channel required on Android 8.0+.
     * Safe to call multiple times — the system ignores duplicate channels.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders for upcoming calendar events"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a reminder notification for an upcoming event.
     *
     * Tapping the notification opens [MainActivity].
     *
     * @param eventId         Used as the unique notification ID.
     * @param title           Event title displayed in the notification.
     * @param startTimeMillis Start time for the content text.
     */
    fun showEventNotification(eventId: Long, title: String, startTimeMillis: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming: $title")
            .setContentText("Starting soon")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(eventId.toInt(), notification)
    }
}
