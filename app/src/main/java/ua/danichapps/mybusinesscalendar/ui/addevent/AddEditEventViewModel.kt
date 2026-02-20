package ua.danichapps.mybusinesscalendar.ui.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult
import ua.danichapps.mybusinesscalendar.domain.model.onError
import ua.danichapps.mybusinesscalendar.domain.model.onSuccess
import ua.danichapps.mybusinesscalendar.domain.repository.CalendarEventRepository
import ua.danichapps.mybusinesscalendar.domain.usecase.AddEventUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.UpdateEventUseCase

/**
 * ViewModel for add / edit event screen.
 *
 * **State vs Events:**
 * - [uiState] — form data that survives configuration change.
 * - [events]  — one-shot events via [Channel] (navigate back, show error).
 *   Never stored in state, so they are not re-triggered after process death.
 */
class AddEditEventViewModel(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val repository: CalendarEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEventUiState())
    val uiState: StateFlow<AddEditEventUiState> = _uiState.asStateFlow()

    /** Buffered channel — each event is delivered to exactly one collector. */
    private val _events = Channel<AddEditEventUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEventUiEvent> = _events.receiveAsFlow()

    // ── Initialisation ─────────────────────────────────────────────────────────

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val event = repository.getEventById(id)
            if (event != null) {
                _uiState.update {
                    it.copy(
                        isLoading                 = false,
                        editingEventId            = event.id,
                        title                     = event.title,
                        description               = event.description,
                        startTimeMillis           = event.startTimeMillis,
                        endTimeMillis             = event.endTimeMillis,
                        isAllDay                  = event.isAllDay,
                        notificationMinutesBefore = event.notificationMinutesBefore,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddEditEventUiEvent.ShowError("Event not found"))
            }
        }
    }

    fun setInitialDay(dayMillis: Long) {
        _uiState.update {
            it.copy(
                startTimeMillis = dayMillis,
                endTimeMillis   = dayMillis + 60 * 60 * 1_000L,
            )
        }
    }

    // ── Form field mutations ───────────────────────────────────────────────────

    fun onTitleChange(value: String)          = _uiState.update { it.copy(title = value, titleError = null) }
    fun onDescriptionChange(value: String)    = _uiState.update { it.copy(description = value) }
    fun onStartTimeChange(millis: Long)       = _uiState.update { it.copy(startTimeMillis = millis) }
    fun onEndTimeChange(millis: Long)         = _uiState.update { it.copy(endTimeMillis = millis) }
    fun onIsAllDayChange(value: Boolean)      = _uiState.update { it.copy(isAllDay = value) }
    fun onNotificationMinutesChange(min: Int) = _uiState.update { it.copy(notificationMinutesBefore = min) }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            return
        }

        // Capture all values before entering the coroutine to avoid stale-state reads
        val eventId     = state.editingEventId
        val event       = CalendarEvent(
            id                        = eventId ?: 0L,
            title                     = state.title.trim(),
            description               = state.description.trim(),
            startTimeMillis           = state.startTimeMillis,
            endTimeMillis             = state.endTimeMillis,
            isAllDay                  = state.isAllDay,
            notificationMinutesBefore = state.notificationMinutesBefore,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result: DomainResult<Unit> = if (eventId != null) {
                updateEventUseCase(event)
            } else {
                addEventUseCase(event).let { r ->
                    when (r) {
                        is DomainResult.Success -> DomainResult.Success(Unit)
                        is DomainResult.Error   -> r
                    }
                }
            }

            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AddEditEventUiEvent.NavigateBack)
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AddEditEventUiEvent.ShowError(msg))
                }
        }
    }
}
