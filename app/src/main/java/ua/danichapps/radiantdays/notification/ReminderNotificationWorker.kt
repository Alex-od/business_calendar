package ua.danichapps.radiantdays.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.danichapps.radiantdays.domain.model.ReminderPolicy
import ua.danichapps.radiantdays.domain.model.displayHeadline
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class ReminderNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: CalendarEventRepository by inject()
    private val notificationManager: EventNotificationManager by inject()
    private val notifierStore = ReminderNotifierStore(context)

    override suspend fun doWork(): Result {
        val eventId = inputData.getLong(ReminderContract.WORK_DATA_EVENT_ID, -1L)
        val fireTimeMillis = inputData.getLong(ReminderContract.WORK_DATA_FIRE_TIME, -1L)
        if (eventId < 0L || fireTimeMillis < 0L) return Result.failure()

        val event = repository.getEventById(eventId) ?: return Result.success()
        val currentFire = ReminderPolicy.reminderFireTimeMillis(event)
        if (currentFire != fireTimeMillis || !ReminderPolicy.shouldScheduleReminder(event)) {
            return Result.success()
        }
        if (notifierStore.wasShown(eventId, fireTimeMillis)) {
            return Result.success()
        }

        notificationManager.showReminderNotification(
            eventId = event.id,
            text = event.displayHeadline(),
            fireTimeMillis = fireTimeMillis,
        )
        notifierStore.markShown(eventId, fireTimeMillis)
        return Result.success()
    }
}
