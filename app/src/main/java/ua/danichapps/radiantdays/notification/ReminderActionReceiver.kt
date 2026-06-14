package ua.danichapps.radiantdays.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase

class ReminderActionReceiver : BroadcastReceiver(), KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository: CalendarEventRepository by inject()
    private val updateEventUseCase: UpdateEventUseCase by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        val eventId = intent?.getLongExtra(ReminderContract.EXTRA_EVENT_ID, -1L) ?: return
        if (eventId < 0L) return

        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ReminderContract.ACTION_COMPLETE -> complete(context, eventId)
                    ReminderContract.ACTION_SNOOZE_5 -> snooze(context, eventId, 5)
                    ReminderContract.ACTION_SNOOZE_10 -> snooze(context, eventId, 10)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun complete(context: Context, eventId: Long) {
        val event = repository.getEventById(eventId) ?: return
        updateEventUseCase(event.copy(isCompleted = true))
        alarmScheduler.cancel(eventId)
        dismiss(context, eventId)
    }

    private suspend fun snooze(context: Context, eventId: Long, minutes: Int) {
        val event = repository.getEventById(eventId) ?: return
        val alarm = event.alarmTimeMillis ?: return
        val updated = event.copy(
            alarmTimeMillis = alarm + minutes * 60_000L,
            isCompleted = false,
        )
        updateEventUseCase(updated)
        ReminderNotifierStore(context).clear(eventId)
        alarmScheduler.schedule(updated)
        dismiss(context, eventId)
    }

    private fun dismiss(context: Context, eventId: Long) {
        NotificationManagerCompat.from(context).cancel(eventId.toInt())
    }
}
