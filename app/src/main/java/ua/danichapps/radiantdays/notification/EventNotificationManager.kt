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
import ua.danichapps.radiantdays.locale.AppLocaleStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = appContext.getString(R.string.notification_channel_description)
            }
            appContext.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        eventId: Long,
        text: String,
        fireTimeMillis: Long,
    ) {
        val locale = AppLocaleStore(appContext).resolveLocale(appContext)
        val timeLabel = SimpleDateFormat("dd MMM, HH:mm", locale)
            .format(Date(fireTimeMillis))

        val contentIntent = PendingIntent.getActivity(
            appContext,
            eventId.toInt(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderContract.EXTRA_EVENT_ID, eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(text)
            .setContentText(appContext.getString(R.string.notification_reminder, timeLabel))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                appContext.getString(R.string.event_completed),
                actionPendingIntent(ReminderContract.ACTION_COMPLETE, eventId),
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                appContext.getString(R.string.notification_snooze_5),
                actionPendingIntent(ReminderContract.ACTION_SNOOZE_5, eventId),
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                appContext.getString(R.string.notification_snooze_10),
                actionPendingIntent(ReminderContract.ACTION_SNOOZE_10, eventId),
            )
            .build()

        NotificationManagerCompat.from(appContext).notify(eventId.toInt(), notification)
    }

    private fun actionPendingIntent(action: String, eventId: Long): PendingIntent {
        val intent = Intent(appContext, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderContract.EXTRA_EVENT_ID, eventId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            "$action$eventId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
    }
}
