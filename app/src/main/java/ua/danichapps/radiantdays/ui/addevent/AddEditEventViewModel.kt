package ua.danichapps.radiantdays.ui.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater
import java.util.Calendar

private const val DEFAULT_ALARM_OFFSET_HOURS = 1L
private const val AUTO_SAVE_DEBOUNCE_MS = 500L

class AddEditEventViewModel(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val getVisibleAiActionsUseCase: GetVisibleAiActionsUseCase,
    private val runAiActionUseCase: RunAiActionUseCase,
    private val repository: CalendarEventRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEventUiState())
    val uiState: StateFlow<AddEditEventUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditEventUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEventUiEvent> = _events.receiveAsFlow()

    private var autoSaveJob: Job? = null

    init {
        observeTags()
        observeVisibleAiActions()
    }

    fun onAiButtonClick() {
        if (_uiState.value.description.isBlank()) return
        _uiState.update { it.copy(aiSheetVisible = true) }
    }

    fun onAiSheetDismiss() {
        _uiState.update { it.copy(aiSheetVisible = false) }
    }

    fun onAiActionSelected(actionGuid: String) {
        val state = _uiState.value
        _uiState.update { it.copy(aiSheetVisible = false, aiLoading = true) }
        viewModelScope.launch {
            val tagNames = state.tags
                .filter { tag -> tag.guid in state.selectedTagGuids }
                .map { tag -> tag.name }
            val context = AiNoteContext(
                text = state.description,
                title = state.title,
                tagNames = tagNames,
                noteDateMillis = state.startTimeMillis,
            )
            runAiActionUseCase(actionGuid, context)
                .onSuccess { result ->
                    _uiState.update { it.copy(aiLoading = false, aiResultText = result) }
                }
                .onError { _, message ->
                    _uiState.update { it.copy(aiLoading = false) }
                    _events.send(AddEditEventUiEvent.ShowError(message))
                }
        }
    }

    fun onAiResultDismiss() {
        _uiState.update { it.copy(aiResultText = null) }
    }

    fun onAiResultReplace() {
        val result = _uiState.value.aiResultText ?: return
        onDescriptionChange(result)
        onAiResultDismiss()
    }

    fun onAiResultAppend() {
        val result = _uiState.value.aiResultText ?: return
        val current = _uiState.value.description
        val separator = when {
            current.isBlank() -> ""
            current.endsWith(' ') || current.endsWith('\n') -> ""
            else -> " "
        }
        onDescriptionChange(current + separator + result)
        onAiResultDismiss()
    }

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val event = repository.getEventById(id)
            if (event != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        editingEventId = event.id,
                        title = event.title,
                        description = event.description,
                        startTimeMillis = event.startTimeMillis,
                        notificationMinutesBefore = event.notificationMinutesBefore,
                        alarmTimeMillis = event.alarmTimeMillis,
                        isCompleted = event.isCompleted,
                        selectedTagGuids = event.tagGuids,
                        createdAtMillis = event.createdAtMillis,
                        updatedAtMillis = event.updatedAtMillis,
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

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
        scheduleAutoSave()
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, descriptionError = null) }
        scheduleAutoSave()
    }

    fun onStartTimeChange(millis: Long) {
        _uiState.update { it.copy(startTimeMillis = millis) }
        scheduleAutoSave()
    }

    fun onAddAlarmClick() {
        _uiState.update { state ->
            if (state.alarmTimeMillis != null) return@update state
            state.copy(alarmTimeMillis = System.currentTimeMillis() + DEFAULT_ALARM_OFFSET_HOURS * 3_600_000L)
        }
        scheduleAutoSave()
    }

    fun onRemoveAlarmClick() {
        _uiState.update { it.copy(alarmTimeMillis = null) }
        scheduleAutoSave()
    }

    fun onAlarmTimeChange(millis: Long) {
        _uiState.update { it.copy(alarmTimeMillis = millis) }
        scheduleAutoSave()
    }

    fun onIsCompletedChange(value: Boolean) {
        _uiState.update { it.copy(isCompleted = value) }
        scheduleAutoSave()
    }

    fun onNotificationMinutesChange(min: Int) {
        _uiState.update { it.copy(notificationMinutesBefore = min) }
        scheduleAutoSave()
    }

    fun onTagToggle(tagGuid: String) {
        _uiState.update { state ->
            val next = if (tagGuid in state.selectedTagGuids) {
                state.selectedTagGuids - tagGuid
            } else {
                state.selectedTagGuids + tagGuid
            }
            state.copy(selectedTagGuids = next)
        }
        scheduleAutoSave()
    }

    fun onTagAddedFromSettings(tagGuid: String) {
        _uiState.update { state ->
            state.copy(selectedTagGuids = state.selectedTagGuids + tagGuid)
        }
        scheduleAutoSave()
    }

    fun onTagsExpandedToggle() = _uiState.update { it.copy(tagsExpanded = !it.tagsExpanded) }

    fun onBackClick() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave(_uiState.value)
            _events.send(AddEditEventUiEvent.NavigateBack)
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            performSave(_uiState.value)
        }
    }

    private suspend fun performSave(state: AddEditEventUiState) {
        if (state.title.isBlank() && state.description.isBlank()) return
        val event = buildEvent(state)
        if (state.editingEventId != null) {
            updateEventUseCase(event)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    _uiState.update { current ->
                        current.copy(
                            createdAtMillis = current.createdAtMillis ?: now,
                            updatedAtMillis = now,
                        )
                    }
                    if (event.alarmTimeMillis == null || event.isCompleted) {
                        alarmScheduler.cancel(event.id)
                    } else {
                        alarmScheduler.schedule(event)
                    }
                    widgetUpdater.refresh()
                }
                .onError { _, msg ->
                    _events.send(AddEditEventUiEvent.ShowError(msg))
                }
        } else {
            addEventUseCase(event)
                .onSuccess { newId ->
                    val now = System.currentTimeMillis()
                    val saved = event.copy(id = newId)
                    _uiState.update {
                        it.copy(
                            editingEventId = newId,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        )
                    }
                    if (saved.alarmTimeMillis != null && !saved.isCompleted) {
                        alarmScheduler.schedule(saved)
                    }
                    widgetUpdater.refresh()
                }
                .onError { _, msg ->
                    _events.send(AddEditEventUiEvent.ShowError(msg))
                }
        }
    }

    private fun buildEvent(state: AddEditEventUiState) = CalendarEvent(
        id = state.editingEventId ?: 0L,
        title = state.title.trim(),
        description = state.description.trim(),
        startTimeMillis = state.startTimeMillis,
        endTimeMillis = state.startTimeMillis + 60 * 60 * 1_000L,
        isAllDay = false,
        notificationMinutesBefore = state.notificationMinutesBefore,
        alarmTimeMillis = state.alarmTimeMillis,
        isCompleted = state.isCompleted,
        tagGuids = state.selectedTagGuids,
    )

    private fun observeTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch { throwable ->
                    _events.send(
                        AddEditEventUiEvent.ShowError(throwable.message ?: "Не удалось загрузить теги"),
                    )
                }
                .collect { tags ->
                    _uiState.update { state ->
                        val validGuids = state.selectedTagGuids.filter { guid ->
                            tags.any { tag -> tag.guid == guid }
                        }.toSet()
                        state.copy(tags = tags, selectedTagGuids = validGuids)
                    }
                }
        }
    }

    private fun observeVisibleAiActions() {
        viewModelScope.launch {
            getVisibleAiActionsUseCase()
                .catch { throwable ->
                    _events.send(
                        AddEditEventUiEvent.ShowError(
                            throwable.message ?: "Не удалось загрузить AI-действия",
                        ),
                    )
                }
                .collect { actions ->
                    _uiState.update { it.copy(visibleAiActions = actions) }
                }
        }
    }
}
