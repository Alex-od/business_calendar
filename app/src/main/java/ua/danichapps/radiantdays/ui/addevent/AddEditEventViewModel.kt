package ua.danichapps.radiantdays.ui.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetFoldersUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import java.util.Calendar

/**
 * ViewModel for add / edit event screen.
 *
 * **State vs Events:**
 * - [uiState] вЂ” form data that survives configuration change.
 * - [events]  вЂ” one-shot events via [Channel] (navigate back, show error).
 *   Never stored in state, so they are not re-triggered after process death.
 */
class AddEditEventViewModel(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val repository: CalendarEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEventUiState())
    val uiState: StateFlow<AddEditEventUiState> = _uiState.asStateFlow()

    /** Buffered channel вЂ” each event is delivered to exactly one collector. */
    private val _events = Channel<AddEditEventUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEventUiEvent> = _events.receiveAsFlow()

    init {
        observeFolders()
    }

    // в”Ђв”Ђ Initialisation в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val event = repository.getEventById(id)
            if (event != null) {
                _uiState.update {
                    it.copy(
                        isLoading                 = false,
                        editingEventId            = event.id,
                        description               = event.description,
                        startTimeMillis           = event.startTimeMillis,
                        notificationMinutesBefore = event.notificationMinutesBefore,
                        isCompleted               = event.isCompleted,
                        selectedFolderGuid        = event.folderGuid,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddEditEventUiEvent.ShowError("Note not found"))
            }
        }
    }

    fun setInitialDay(dayMillis: Long) {
        val startAt9 = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        _uiState.update { it.copy(startTimeMillis = startAt9) }
    }

    // в”Ђв”Ђ Form field mutations в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    fun onDescriptionChange(value: String)    = _uiState.update { it.copy(description = value, descriptionError = null) }
    fun onStartTimeChange(millis: Long)       = _uiState.update { it.copy(startTimeMillis = millis) }
    fun onIsCompletedChange(value: Boolean)   = _uiState.update { it.copy(isCompleted = value) }
    fun onNotificationMinutesChange(min: Int) = _uiState.update { it.copy(notificationMinutesBefore = min) }
    fun onFolderSelected(folderGuid: String?)  = _uiState.update { it.copy(selectedFolderGuid = folderGuid) }

    // в”Ђв”Ђ Save в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    fun save() {
        val state = _uiState.value
        if (state.description.isBlank()) {
            _uiState.update { it.copy(descriptionError = "Note text is required") }
            return
        }

        val eventId = state.editingEventId
        val event = CalendarEvent(
            id                        = eventId ?: 0L,
            description               = state.description.trim(),
            startTimeMillis           = state.startTimeMillis,
            endTimeMillis             = state.startTimeMillis + 60 * 60 * 1_000L,
            isAllDay                  = false,
            notificationMinutesBefore = state.notificationMinutesBefore,
            isCompleted               = state.isCompleted,
            folderGuid                = state.selectedFolderGuid,
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

    private fun observeFolders() {
        viewModelScope.launch {
            getFoldersUseCase()
                .catch { throwable ->
                    _events.send(
                        AddEditEventUiEvent.ShowError(throwable.message ?: "Folders loading failed"),
                    )
                }
                .collect { folders ->
                    _uiState.update { state ->
                        val selectedGuid = state.selectedFolderGuid
                        state.copy(
                            folders = folders,
                            selectedFolderGuid = selectedGuid?.takeIf { guid ->
                                folders.any { folder -> folder.guid == guid }
                            },
                        )
                    }
                }
        }
    }
}
