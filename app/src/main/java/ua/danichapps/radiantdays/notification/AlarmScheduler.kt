package ua.danichapps.radiantdays.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.ReminderPolicy
import ua.danichapps.radiantdays.domain.usecase.GetPendingRemindersUseCase
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AlarmScheduler(
    private val context: Context,
    private val getPendingRemindersUseCase: GetPendingRemindersUseCase,
) {

    private val workManager = WorkManager.getInstance(context)

    fun schedule(event: CalendarEvent) {
        if (!ReminderPolicy.shouldScheduleReminder(event)) {
            cancel(event.id)
            return
        }
        val fireMillis = ReminderPolicy.reminderFireTimeMillis(event) ?: return
        val delayMillis = max(0L, fireMillis - System.currentTimeMillis())
        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderContract.WORK_DATA_EVENT_ID to event.id,
                    ReminderContract.WORK_DATA_FIRE_TIME to fireMillis,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(
            ReminderContract.uniqueWorkName(event.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(eventId: Long) {
        workManager.cancelUniqueWork(ReminderContract.uniqueWorkName(eventId))
        ReminderNotifierStore(context).clear(eventId)
    }

    suspend fun rescheduleAll() {
        when (val result = getPendingRemindersUseCase(System.currentTimeMillis())) {
            is DomainResult.Success -> result.data.forEach(::schedule)
            is DomainResult.Error -> Unit
        }
    }
}
