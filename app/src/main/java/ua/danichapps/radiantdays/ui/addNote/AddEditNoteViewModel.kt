package ua.danichapps.radiantdays.ui.addNote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.normalizeFirstUserMessage
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventByIdUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater

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
    localeStore: AppLocaleStore,
    apiKeyStore: AiApiKeyStore,
    private val noteEditorPreferencesStore: NoteEditorPreferencesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditNoteUiState())
    val uiState: StateFlow<AddEditNoteUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditNoteUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditNoteUiEvent> = _events.receiveAsFlow()

    private val saveCoordinator = NoteSaveCoordinator(
        addEventUseCase = addEventUseCase,
        updateEventUseCase = updateEventUseCase,
        alarmScheduler = alarmScheduler,
        widgetUpdater = widgetUpdater,
        updateState = { transform -> _uiState.update(transform) },
        onSaveError = { key, args, cause ->
            _events.send(AddEditNoteUiEvent.ShowError(key, args, cause))
        },
    )
    private val autoSave = NoteAutoSaveController(
        scope = viewModelScope,
        debounceMs = AUTO_SAVE_DEBOUNCE_MS,
        readState = { _uiState.value },
        save = saveCoordinator::save,
    )
    private val editor = NoteEditorFeature(
        scope = viewModelScope,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
        onScheduleAutoSave = autoSave::schedule,
    )
    private val tagsState: StateFlow<List<Tag>> = getTagsUseCase()
        .catch {
            _events.send(AddEditNoteUiEvent.ShowError(MessageKey.LOAD_TAGS_FAILED))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val ai = NoteAiOrchestrator(
        scope = viewModelScope,
        apiKeyStore = apiKeyStore,
        localeStore = localeStore,
        getVisibleAiActionsUseCase = getVisibleAiActionsUseCase,
        runAiActionUseCase = runAiActionUseCase,
        continueAiChatUseCase = continueAiChatUseCase,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
        onShowError = { key, args, cause ->
            _events.send(AddEditNoteUiEvent.ShowError(key, args, cause))
        },
        onScheduleAutoSave = autoSave::schedule,
        onSyncDescriptionFromFirstChatMessage = editor::applyDiscreteDescriptionChange,
    )

    init {
        ai.refreshKeyStatus()
        refreshNoteEditorPreferences()
        observeTags()
        observeVisibleActions()
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
                editor.clearUndoHistory()
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
                _events.send(AddEditNoteUiEvent.ShowError(MessageKey.NOTE_NOT_FOUND))
            }
        }
    }

    /** Sets start time to 09:00 on the given day for a new event. */
    fun setInitialDay(dayMillis: Long) {
        editor.setInitialDay(dayMillis)
    }

    /** Updates description with grouped undo support for typing bursts. */
    fun onDescriptionChange(value: String) {
        editor.onDescriptionChange(value)
    }

    /** Applies voice input as a single undoable description change. */
    fun onDescriptionChangeFromVoice(value: String) {
        editor.onDescriptionChangeFromVoice(value)
    }

    /** Reverts description to the previous undo snapshot or in-progress group. */
    fun onDescriptionUndo() {
        editor.onDescriptionUndo()
    }

    /** Sets default alarm one hour ahead if none exists. */
    fun onAddAlarmClick() {
        editor.onAddAlarmClick()
    }

    /** Clears the alarm for this event. */
    fun onRemoveAlarmClick() {
        editor.onRemoveAlarmClick()
    }

    /** Updates alarm date/time. */
    fun onAlarmTimeChange(millis: Long) {
        editor.onAlarmTimeChange(millis)
    }

    /** Toggles event completed flag. */
    fun onIsCompletedChange(value: Boolean) {
        editor.onIsCompletedChange(value)
    }

    /** Sets how many minutes before the alarm to notify. */
    fun onNotificationMinutesChange(min: Int) {
        editor.onNotificationMinutesChange(min)
    }

    /** Adds or removes a tag from the selected set. */
    fun onTagToggle(tagGuid: String) {
        editor.onTagToggle(tagGuid)
    }

    /** Selects a tag created in settings and returns to this screen. */
    fun onTagAddedFromSettings(tagGuid: String) {
        editor.onTagAddedFromSettings(tagGuid)
    }

    /** Expands or collapses the unselected tags row. */
    fun onTagsExpandedToggle() = editor.onTagsExpandedToggle()

    private var isNavigatingBack = false

    /** Flushes pending save and navigates back. */
    fun onBackClick() {
        if (isNavigatingBack) return
        isNavigatingBack = true
        autoSave.cancel()
        viewModelScope.launch {
            try {
                autoSave.flushAndSave()
                _events.send(AddEditNoteUiEvent.NavigateBack)
            } catch (_: Exception) {
                isNavigatingBack = false
            }
        }
    }

    /** Subscribes to tag list and prunes stale selected guids. */
    private fun observeTags() {
        viewModelScope.launch {
            tagsState.collect { tags ->
                editor.applyAvailableTags(tags)
            }
        }
    }

    private fun observeVisibleActions() {
        viewModelScope.launch {
            ai.visibleActions.collect { actions ->
                _uiState.update { it.copy(visibleAiActions = actions) }
            }
        }
    }
}
