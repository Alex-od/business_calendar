package ua.danichapps.radiantdays.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.ReminderPolicy
import ua.danichapps.radiantdays.domain.model.displayHeadline
import ua.danichapps.radiantdays.domain.usecase.GetUpcomingEventsUseCase

/**
 * Infrequent safety net if a one-time reminder work was dropped by the system.
 */
class ReminderFallbackWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val getUpcomingEventsUseCase: GetUpcomingEventsUseCase by inject()
    private val notificationManager: EventNotificationManager by inject()
    private val notifierStore = ReminderNotifierStore(context)
    private val alarmScheduler: AlarmScheduler by inject()

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val windowStart = now - 30 * 60_000L
        val windowEnd = now + 5 * 60_000L

        when (val result = getUpcomingEventsUseCase(windowStart, windowEnd)) {
            is DomainResult.Success -> {
                result.data.forEach { event ->
                    val fireMillis = ReminderPolicy.reminderFireTimeMillis(event) ?: return@forEach
                    if (notifierStore.wasShown(event.id, fireMillis)) return@forEach
                    notificationManager.showReminderNotification(
                        eventId = event.id,
                        text = event.displayHeadline(),
                        fireTimeMillis = fireMillis,
                    )
                    notifierStore.markShown(event.id, fireMillis)
                }
            }
            is DomainResult.Error -> Unit
        }

        alarmScheduler.rescheduleAll()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "reminder_fallback_periodic"

        fun schedule(context: Context) {
            val request = androidx.work.PeriodicWorkRequestBuilder<ReminderFallbackWorker>(
                12,
                java.util.concurrent.TimeUnit.HOURS,
            ).build()
            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
