package ua.danichapps.radiantdays.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ua.danichapps.radiantdays.MainActivity
import ua.danichapps.radiantdays.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventNotificationManager(private val context: Context) {

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Напоминания о заметках"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        eventId: Long,
        text: String,
        fireTimeMillis: Long,
    ) {
        val timeLabel = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(fireTimeMillis))

        val contentIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderContract.EXTRA_EVENT_ID, eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(text)
            .setContentText("Напоминание: $timeLabel")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Выполнено",
                actionPendingIntent(ReminderContract.ACTION_COMPLETE, eventId),
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "+5 мин",
                actionPendingIntent(ReminderContract.ACTION_SNOOZE_5, eventId),
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "+10 мин",
                actionPendingIntent(ReminderContract.ACTION_SNOOZE_10, eventId),
            )
            .build()

        NotificationManagerCompat.from(context).notify(eventId.toInt(), notification)
    }

    private fun actionPendingIntent(action: String, eventId: Long): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderContract.EXTRA_EVENT_ID, eventId)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action$eventId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val CHANNEL_NAME = "Напоминания"
    }
}
