package ua.danichapps.radiantdays.ui.addNote

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater

/** Persists note changes and syncs alarm scheduling and the home-screen widget. */
internal class NoteSaveCoordinator(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
    private val updateState: ((AddEditNoteUiState) -> AddEditNoteUiState) -> Unit,
    private val onSaveError: suspend (MessageKey, List<String>, Throwable?) -> Unit,
) {
    suspend fun save(state: AddEditNoteUiState) {
        if (state.title.isBlank() && state.description.isBlank() && state.aiChatMessages.isEmpty()) return
        val event = state.toCalendarEvent()
        if (state.editingNoteId != null) {
            updateEventUseCase(event)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    updateState { current ->
                        current.copy(
                            createdAtMillis = current.createdAtMillis ?: now,
                            updatedAtMillis = now,
                        )
                    }
                    syncAlarm(event)
                    widgetUpdater.refresh()
                }
                .onError { exception, key, args ->
                    onSaveError(key, args, exception)
                }
        } else {
            addEventUseCase(event)
                .onSuccess { newId ->
                    val now = System.currentTimeMillis()
                    val saved = event.copy(id = newId)
                    updateState {
                        it.copy(
                            editingNoteId = newId,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        )
                    }
                    if (saved.alarmTimeMillis != null && !saved.isCompleted) {
                        alarmScheduler.schedule(saved)
                    }
                    widgetUpdater.refresh()
                }
                .onError { exception, key, args ->
                    onSaveError(key, args, exception)
                }
        }
    }

    private fun syncAlarm(event: CalendarEvent) {
        if (event.alarmTimeMillis == null || event.isCompleted) {
            alarmScheduler.cancel(event.id)
        } else {
            alarmScheduler.schedule(event)
        }
    }
}

private fun AddEditNoteUiState.toCalendarEvent() = CalendarEvent(
    id = editingNoteId ?: 0L,
    title = title.trim(),
    description = description.trim(),
    startTimeMillis = startTimeMillis,
    endTimeMillis = startTimeMillis + 60 * 60 * 1_000L,
    isAllDay = false,
    notificationMinutesBefore = notificationMinutesBefore,
    alarmTimeMillis = alarmTimeMillis,
    isCompleted = isCompleted,
    tagGuids = selectedTagGuids,
    aiChatMessages = aiChatMessages,
)
