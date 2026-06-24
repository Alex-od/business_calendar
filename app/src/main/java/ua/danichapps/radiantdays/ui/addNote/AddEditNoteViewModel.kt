package ua.danichapps.radiantdays.ui.addNote

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
import ua.danichapps.radiantdays.domain.model.normalizeFirstUserMessage
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventByIdUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.locale.DomainErrorStrings
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater
import java.util.Calendar

private const val DEFAULT_ALARM_OFFSET_HOURS = 1L
private const val AUTO_SAVE_DEBOUNCE_MS = 500L

class AddEditNoteViewModel(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    getVisibleAiActionsUseCase: GetVisibleAiActionsUseCase,
    runAiActionUseCase: RunAiActionUseCase,
    continueAiChatUseCase: ContinueAiChatUseCase,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
    private val errorStrings: DomainErrorStrings,
    localeStore: AppLocaleStore,
    apiKeyStore: AiApiKeyStore,
    private val noteEditorPreferencesStore: NoteEditorPreferencesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditNoteUiState())
    val uiState: StateFlow<AddEditNoteUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditNoteUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditNoteUiEvent> = _events.receiveAsFlow()

    private val descriptionUndo = NoteDescriptionUndoController(viewModelScope)
    private val autoSave = NoteAutoSaveController(
        scope = viewModelScope,
        debounceMs = AUTO_SAVE_DEBOUNCE_MS,
        readState = { _uiState.value },
        save = ::performSave,
    )
    private val ai = NoteAiOrchestrator(
        scope = viewModelScope,
        apiKeyStore = apiKeyStore,
        localeStore = localeStore,
        getVisibleAiActionsUseCase = getVisibleAiActionsUseCase,
        runAiActionUseCase = runAiActionUseCase,
        continueAiChatUseCase = continueAiChatUseCase,
        errorStrings = errorStrings,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
        onShowError = { message -> _events.send(AddEditNoteUiEvent.ShowError(message)) },
        onScheduleAutoSave = autoSave::schedule,
        onSyncDescriptionFromFirstChatMessage = ::applyDiscreteDescriptionChange,
    )

    init {
        ai.refreshKeyStatus()
        refreshNoteEditorPreferences()
        observeTags()
        ai.observeVisibleActions()
    }

    /** Reloads format toolbar and AI chat visibility from preferences. */
    fun refreshNoteEditorPreferences() {
        _uiState.update {
            it.copy(
                showFormatToolbar = noteEditorPreferencesStore.isFormatToolbarVisible(),
                showAiChat = noteEditorPreferencesStore.isAiChatVisible(),
            )
        }
    }

    /** Shows or hides the note format toolbar and persists the choice. */
    fun onShowFormatToolbarChange(visible: Boolean) {
        noteEditorPreferencesStore.setFormatToolbarVisible(visible)
        _uiState.update { it.copy(showFormatToolbar = visible) }
    }

    /** Shows or hides the inline AI chat panel and persists the choice. */
    fun onShowAiChatChange(visible: Boolean) {
        noteEditorPreferencesStore.setAiChatVisible(visible)
        _uiState.update { it.copy(showAiChat = visible) }
    }

    /** Updates whether an AI API key is configured. */
    fun refreshAiKeyStatus() = ai.refreshKeyStatus()

    /** Opens the AI actions sheet when key and description are present. */
    fun onAiButtonClick() = ai.onAiButtonClick()

    /** Opens the AI actions sheet from the chat input bar. */
    fun openAiActionsSheet() = ai.openActionsSheet()

    /** Closes the AI actions bottom sheet. */
    fun onAiSheetDismiss() = ai.dismissActionsSheet()

    /** Runs the selected AI action and appends user/assistant messages. */
    fun onAiActionSelected(actionGuid: String) = ai.onActionSelected(actionGuid)

    /** Edits a chat message; syncs the first user message back to the note. */
    fun onAiChatMessageEdit(index: Int, content: String) = ai.onChatMessageEdit(index, content)

    /** Removes a chat message and its paired action prompt when applicable. */
    fun onAiChatMessageDelete(index: Int) = ai.onChatMessageDelete(index)

    /** Sends a free-form follow-up message in the AI chat. */
    fun onAiChatSend(message: String) = ai.onChatSend(message)

    /** Loads an existing event by id into uiState. */
    fun loadNote(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val event = getEventByIdUseCase(id)
            if (event != null) {
                descriptionUndo.clear(::updateCanUndoDescription)
                val chatMessages = event.aiChatMessages.normalizeFirstUserMessage(event.description)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        editingNoteId = event.id,
                        title = event.title,
                        description = event.description,
                        startTimeMillis = event.startTimeMillis,
                        notificationMinutesBefore = event.notificationMinutesBefore,
                        alarmTimeMillis = event.alarmTimeMillis,
                        isCompleted = event.isCompleted,
                        selectedTagGuids = event.tagGuids,
                        aiChatMessages = chatMessages,
                        createdAtMillis = event.createdAtMillis,
                        updatedAtMillis = event.updatedAtMillis,
                    )
                }
                if (chatMessages != event.aiChatMessages) {
                    autoSave.schedule()
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddEditNoteUiEvent.ShowError(errorStrings.resolve(MessageKey.NOTE_NOT_FOUND)))
            }
        }
    }

    /** Sets start time to 09:00 on the given day for a new event. */
    fun setInitialDay(dayMillis: Long) {
        descriptionUndo.clear(::updateCanUndoDescription)
        val startAt9 = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        _uiState.update { it.copy(startTimeMillis = startAt9) }
    }

    /** Updates description with grouped undo support for typing bursts. */
    fun onDescriptionChange(value: String) {
        val current = _uiState.value.description
        val next = descriptionUndo.onTypingChange(value, current, ::updateCanUndoDescription) ?: return
        setDescription(next)
    }

    /** Applies voice input as a single undoable description change. */
    fun onDescriptionChangeFromVoice(value: String) {
        applyDiscreteDescriptionChange(value)
    }

    /** Reverts description to the previous undo snapshot or in-progress group. */
    fun onDescriptionUndo() {
        val previous = descriptionUndo.undo(::updateCanUndoDescription) ?: return
        setDescription(previous, scheduleSave = false)
    }

    /** Updates event start time. */
    fun onStartTimeChange(millis: Long) {
        _uiState.update { it.copy(startTimeMillis = millis) }
        autoSave.schedule()
    }

    /** Sets default alarm one hour ahead if none exists. */
    fun onAddAlarmClick() {
        _uiState.update { state ->
            if (state.alarmTimeMillis != null) return@update state
            state.copy(alarmTimeMillis = System.currentTimeMillis() + DEFAULT_ALARM_OFFSET_HOURS * 3_600_000L)
        }
        autoSave.schedule()
    }

    /** Clears the alarm for this event. */
    fun onRemoveAlarmClick() {
        _uiState.update { it.copy(alarmTimeMillis = null) }
        autoSave.schedule()
    }

    /** Updates alarm date/time. */
    fun onAlarmTimeChange(millis: Long) {
        _uiState.update { it.copy(alarmTimeMillis = millis) }
        autoSave.schedule()
    }

    /** Toggles event completed flag. */
    fun onIsCompletedChange(value: Boolean) {
        _uiState.update { it.copy(isCompleted = value) }
        autoSave.schedule()
    }

    /** Sets how many minutes before the alarm to notify. */
    fun onNotificationMinutesChange(min: Int) {
        _uiState.update { it.copy(notificationMinutesBefore = min) }
        autoSave.schedule()
    }

    /** Adds or removes a tag from the selected set. */
    fun onTagToggle(tagGuid: String) {
        _uiState.update { state ->
            val next = if (tagGuid in state.selectedTagGuids) {
                state.selectedTagGuids - tagGuid
            } else {
                state.selectedTagGuids + tagGuid
            }
            state.copy(selectedTagGuids = next)
        }
        autoSave.schedule()
    }

    /** Selects a tag created in settings and returns to this screen. */
    fun onTagAddedFromSettings(tagGuid: String) {
        _uiState.update { state ->
            state.copy(selectedTagGuids = state.selectedTagGuids + tagGuid)
        }
        autoSave.schedule()
    }

    /** Expands or collapses the unselected tags row. */
    fun onTagsExpandedToggle() = _uiState.update { it.copy(tagsExpanded = !it.tagsExpanded) }

    /** Flushes pending save and navigates back. */
    fun onBackClick() {
        autoSave.cancel()
        viewModelScope.launch {
            autoSave.flushAndSave()
            _events.send(AddEditNoteUiEvent.NavigateBack)
        }
    }

    private fun setDescription(value: String, scheduleSave: Boolean = true) {
        _uiState.update { it.copy(description = value, descriptionError = null) }
        if (scheduleSave) {
            autoSave.schedule()
        }
    }

    private fun applyDiscreteDescriptionChange(value: String) {
        val current = _uiState.value.description
        val next = descriptionUndo.onDiscreteChange(value, current, ::updateCanUndoDescription) ?: return
        setDescription(next)
    }

    private fun updateCanUndoDescription(canUndo: Boolean) {
        if (_uiState.value.canUndoDescription != canUndo) {
            _uiState.update { it.copy(canUndoDescription = canUndo) }
        }
    }

    /** Creates or updates the event, then refreshes alarm and widget. */
    private suspend fun performSave(state: AddEditNoteUiState) {
        if (state.title.isBlank() && state.description.isBlank() && state.aiChatMessages.isEmpty()) return
        val event = buildNote(state)
        if (state.editingNoteId != null) {
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
                .onError { exception, key, args ->
                    _events.send(AddEditNoteUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
                }
        } else {
            addEventUseCase(event)
                .onSuccess { newId ->
                    val now = System.currentTimeMillis()
                    val saved = event.copy(id = newId)
                    _uiState.update {
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
                    _events.send(AddEditNoteUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
                }
        }
    }

    /** Maps uiState to a [CalendarEvent] for persistence. */
    private fun buildNote(state: AddEditNoteUiState) = CalendarEvent(
        id = state.editingNoteId ?: 0L,
        title = state.title.trim(),
        description = state.description.trim(),
        startTimeMillis = state.startTimeMillis,
        endTimeMillis = state.startTimeMillis + 60 * 60 * 1_000L,
        isAllDay = false,
        notificationMinutesBefore = state.notificationMinutesBefore,
        alarmTimeMillis = state.alarmTimeMillis,
        isCompleted = state.isCompleted,
        tagGuids = state.selectedTagGuids,
        aiChatMessages = state.aiChatMessages,
    )

    /** Subscribes to tag list and prunes stale selected guids. */
    private fun observeTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch {
                    _events.send(
                        AddEditNoteUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.LOAD_TAGS_FAILED),
                        ),
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
}
