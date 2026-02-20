package ua.danichapps.mybusinesscalendar.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult
import ua.danichapps.mybusinesscalendar.domain.usecase.GetUpcomingEventsUseCase
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager task that checks for events starting in the next hour
 * and fires reminder notifications.
 *
 * Implements [KoinComponent] so it can resolve Koin bindings via property
 * injection without needing a custom [androidx.work.WorkerFactory].
 *
 * Schedule: every 15 minutes (minimum WorkManager interval).
 */
class EventNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val getUpcomingEventsUseCase: GetUpcomingEventsUseCase by inject()
    private val notificationManager: EventNotificationManager      by inject()

    override suspend fun doWork(): Result {
        val now       = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1_000L

        val result = getUpcomingEventsUseCase(fromMillis = now, toMillis = now + oneHourMs)

        if (result is DomainResult.Success) {
            result.data.forEach { event ->
                notificationManager.showEventNotification(
                    eventId         = event.id,
                    title           = event.title,
                    startTimeMillis = event.startTimeMillis,
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "event_notification_periodic"

        /**
         * Enqueues the worker as a unique periodic task.
         * Calling this multiple times is idempotent ([ExistingPeriodicWorkPolicy.KEEP]).
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EventNotificationWorker>(
                repeatInterval     = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
