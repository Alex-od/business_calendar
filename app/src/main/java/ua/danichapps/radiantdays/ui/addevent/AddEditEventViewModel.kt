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
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.normalizeFirstUserMessage
import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.locale.DomainErrorStrings
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater
import java.util.Calendar

private const val DEFAULT_ALARM_OFFSET_HOURS = 1L
private const val AUTO_SAVE_DEBOUNCE_MS = 500L
private const val DESCRIPTION_UNDO_LIMIT = 10
private const val DESCRIPTION_UNDO_GROUP_MS = 1_000L

class AddEditEventViewModel(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val getVisibleAiActionsUseCase: GetVisibleAiActionsUseCase,
    private val runAiActionUseCase: RunAiActionUseCase,
    private val continueAiChatUseCase: ContinueAiChatUseCase,
    private val repository: CalendarEventRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
    private val errorStrings: DomainErrorStrings,
    private val localeStore: AppLocaleStore,
    private val apiKeyStore: AiApiKeyStore,
    private val noteEditorPreferencesStore: NoteEditorPreferencesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEventUiState())
    val uiState: StateFlow<AddEditEventUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditEventUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEventUiEvent> = _events.receiveAsFlow()

    private var autoSaveJob: Job? = null

    private val descriptionUndoStack = ArrayDeque<String>()
    private var descriptionUndoGroupBase: String? = null
    private var descriptionUndoGroupJob: Job? = null
    private var isUndoingDescription = false

    init {
        refreshAiKeyStatus()
        refreshNoteEditorPreferences()
        observeTags()
        observeVisibleAiActions()
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
    fun refreshAiKeyStatus() {
        _uiState.update { it.copy(isAiKeySaved = apiKeyStore.hasKey()) }
    }

    /** Opens the AI actions sheet when key and description are present. */
    fun onAiButtonClick() {
        if (!_uiState.value.isAiKeySaved) return
        if (_uiState.value.description.isBlank()) return
        _uiState.update { it.copy(aiSheetVisible = true) }
    }

    /** Opens the AI actions sheet from the chat input bar. */
    fun openAiActionsSheet() {
        _uiState.update { it.copy(aiSheetVisible = true) }
    }

    /** Closes the AI actions bottom sheet. */
    fun onAiSheetDismiss() {
        _uiState.update { it.copy(aiSheetVisible = false) }
    }

    /** Runs the selected AI action and appends user/assistant messages. */
    fun onAiActionSelected(actionGuid: String) {
        val state = _uiState.value
        val history = state.aiChatMessages
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
                locale = localeStore.resolveLocale(),
            )
            runAiActionUseCase(actionGuid, context, history)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            aiLoading = false,
                            aiChatMessages = history +
                                result.userMessage +
                                AiChatMessage(AiChatRole.ASSISTANT, result.response),
                        )
                    }
                    scheduleAutoSave()
                }
                .onError { exception, key, args ->
                    _uiState.update { it.copy(aiLoading = false) }
                    _events.send(AddEditEventUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
                }
        }
    }

    /** Edits a chat message; syncs the first user message back to the note. */
    fun onAiChatMessageEdit(index: Int, content: String) {
        val messages = _uiState.value.aiChatMessages
        if (index !in messages.indices) return
        val current = messages[index]
        if (current.content == content) return
        _uiState.update { state ->
            state.copy(
                aiChatMessages = messages.mapIndexed { i, message ->
                    if (i == index) {
                        message.copy(content = content, apiContent = message.apiContent, actionLabel = message.actionLabel)
                    } else {
                        message
                    }
                },
            )
        }
        if (index == 0 && current.role == AiChatRole.USER) {
            applyDiscreteDescriptionChange(content)
        }
        scheduleAutoSave()
    }

    /** Removes a chat message and its paired action prompt when applicable. */
    fun onAiChatMessageDelete(index: Int) {
        if (_uiState.value.aiChatLoading) return
        val messages = _uiState.value.aiChatMessages
        if (index !in messages.indices) return

        val indicesToRemove = linkedSetOf(index)
        val message = messages[index]
        if (message.role == AiChatRole.ASSISTANT) {
            messages.getOrNull(index - 1)?.let { previous ->
                if (previous.role == AiChatRole.USER && previous.actionLabel != null) {
                    indicesToRemove.add(index - 1)
                }
            }
        }

        _uiState.update { state ->
            state.copy(aiChatMessages = messages.filterIndexed { i, _ -> i !in indicesToRemove })
        }
        scheduleAutoSave()
    }

    /** Sends a free-form follow-up message in the AI chat. */
    fun onAiChatSend(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank() || _uiState.value.aiChatLoading) return

        val history = _uiState.value.aiChatMessages
        val userMessage = AiChatMessage(AiChatRole.USER, trimmed)
        _uiState.update {
            it.copy(
                aiChatMessages = history + userMessage,
                aiChatLoading = true,
            )
        }

        viewModelScope.launch {
            continueAiChatUseCase(history, trimmed)
                .onSuccess { response ->
                    _uiState.update { state ->
                        state.copy(
                            aiChatLoading = false,
                            aiChatMessages = state.aiChatMessages +
                                AiChatMessage(AiChatRole.ASSISTANT, response),
                        )
                    }
                    scheduleAutoSave()
                }
                .onError { exception, key, args ->
                    _uiState.update { state ->
                        state.copy(
                            aiChatLoading = false,
                            aiChatMessages = state.aiChatMessages.dropLast(1),
                        )
                    }
                    _events.send(AddEditEventUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
                }
        }
    }

    /** Loads an existing event by id into uiState. */
    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val event = repository.getEventById(id)
            if (event != null) {
                clearDescriptionUndo()
                val chatMessages = event.aiChatMessages.normalizeFirstUserMessage(event.description)
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
                        aiChatMessages = chatMessages,
                        createdAtMillis = event.createdAtMillis,
                        updatedAtMillis = event.updatedAtMillis,
                    )
                }
                if (chatMessages != event.aiChatMessages) {
                    scheduleAutoSave()
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddEditEventUiEvent.ShowError(errorStrings.resolve(MessageKey.NOTE_NOT_FOUND)))
            }
        }
    }

    /** Sets start time to 09:00 on the given day for a new event. */
    fun setInitialDay(dayMillis: Long) {
        clearDescriptionUndo()
        val startAt9 = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        _uiState.update { it.copy(startTimeMillis = startAt9) }
    }

    /** Updates the event title and clears validation error. */
    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
        scheduleAutoSave()
    }

    /** Updates description with grouped undo support for typing bursts. */
    fun onDescriptionChange(value: String) {
        if (isUndoingDescription) {
            setDescription(value)
            return
        }
        val current = _uiState.value.description
        if (value == current) return

        if (descriptionUndoGroupBase == null) {
            descriptionUndoGroupBase = current
            updateCanUndoDescription()
        }
        setDescription(value)
        scheduleUndoGroupCommit()
    }

    /** Applies voice input as a single undoable description change. */
    fun onDescriptionChangeFromVoice(value: String) {
        applyDiscreteDescriptionChange(value)
    }

    /** Reverts description to the previous undo snapshot or in-progress group. */
    fun onDescriptionUndo() {
        descriptionUndoGroupJob?.cancel()
        descriptionUndoGroupJob = null

        val groupBase = descriptionUndoGroupBase
        if (groupBase != null) {
            descriptionUndoGroupBase = null
            updateCanUndoDescription()
            isUndoingDescription = true
            setDescription(groupBase)
            isUndoingDescription = false
            return
        }

        if (descriptionUndoStack.isEmpty()) return

        flushUndoGroup()
        val previous = descriptionUndoStack.removeLast()
        updateCanUndoDescription()
        isUndoingDescription = true
        setDescription(previous)
        isUndoingDescription = false
    }

    /** Writes description to uiState and schedules auto-save. */
    private fun setDescription(value: String) {
        _uiState.update { it.copy(description = value, descriptionError = null) }
        scheduleAutoSave()
    }

    /** Replaces description atomically with a new undo snapshot. */
    private fun applyDiscreteDescriptionChange(value: String) {
        if (value == _uiState.value.description) return
        flushUndoGroup()
        pushUndoSnapshot(_uiState.value.description)
        descriptionUndoGroupBase = null
        setDescription(value)
    }

    /** Commits the current typing group to the undo stack after a pause. */
    private fun scheduleUndoGroupCommit() {
        descriptionUndoGroupJob?.cancel()
        descriptionUndoGroupJob = viewModelScope.launch {
            delay(DESCRIPTION_UNDO_GROUP_MS)
            val base = descriptionUndoGroupBase ?: return@launch
            pushUndoSnapshot(base)
            descriptionUndoGroupBase = null
            updateCanUndoDescription()
            descriptionUndoGroupJob = null
        }
    }

    /** Adds a description snapshot to the bounded undo stack. */
    private fun pushUndoSnapshot(text: String) {
        if (descriptionUndoStack.lastOrNull() == text) return
        if (descriptionUndoStack.size >= DESCRIPTION_UNDO_LIMIT) {
            descriptionUndoStack.removeFirst()
        }
        descriptionUndoStack.addLast(text)
        updateCanUndoDescription()
    }

    /** Commits any pending typing group before a discrete change or undo. */
    private fun flushUndoGroup() {
        descriptionUndoGroupJob?.cancel()
        descriptionUndoGroupJob = null
        val base = descriptionUndoGroupBase
        if (base != null) {
            pushUndoSnapshot(base)
            descriptionUndoGroupBase = null
            updateCanUndoDescription()
        }
    }

    /** Resets undo state when loading or resetting the note. */
    private fun clearDescriptionUndo() {
        descriptionUndoGroupJob?.cancel()
        descriptionUndoGroupJob = null
        descriptionUndoGroupBase = null
        descriptionUndoStack.clear()
        updateCanUndoDescription()
    }

    /** Updates [AddEditEventUiState.canUndoDescription] from stack and group state. */
    private fun updateCanUndoDescription() {
        val canUndo = descriptionUndoStack.isNotEmpty() || descriptionUndoGroupBase != null
        if (_uiState.value.canUndoDescription != canUndo) {
            _uiState.update { it.copy(canUndoDescription = canUndo) }
        }
    }

    /** Updates event start time. */
    fun onStartTimeChange(millis: Long) {
        _uiState.update { it.copy(startTimeMillis = millis) }
        scheduleAutoSave()
    }

    /** Sets default alarm one hour ahead if none exists. */
    fun onAddAlarmClick() {
        _uiState.update { state ->
            if (state.alarmTimeMillis != null) return@update state
            state.copy(alarmTimeMillis = System.currentTimeMillis() + DEFAULT_ALARM_OFFSET_HOURS * 3_600_000L)
        }
        scheduleAutoSave()
    }

    /** Clears the alarm for this event. */
    fun onRemoveAlarmClick() {
        _uiState.update { it.copy(alarmTimeMillis = null) }
        scheduleAutoSave()
    }

    /** Updates alarm date/time. */
    fun onAlarmTimeChange(millis: Long) {
        _uiState.update { it.copy(alarmTimeMillis = millis) }
        scheduleAutoSave()
    }

    /** Toggles event completed flag. */
    fun onIsCompletedChange(value: Boolean) {
        _uiState.update { it.copy(isCompleted = value) }
        scheduleAutoSave()
    }

    /** Sets how many minutes before the alarm to notify. */
    fun onNotificationMinutesChange(min: Int) {
        _uiState.update { it.copy(notificationMinutesBefore = min) }
        scheduleAutoSave()
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
        scheduleAutoSave()
    }

    /** Selects a tag created in settings and returns to this screen. */
    fun onTagAddedFromSettings(tagGuid: String) {
        _uiState.update { state ->
            state.copy(selectedTagGuids = state.selectedTagGuids + tagGuid)
        }
        scheduleAutoSave()
    }

    /** Expands or collapses the unselected tags row. */
    fun onTagsExpandedToggle() = _uiState.update { it.copy(tagsExpanded = !it.tagsExpanded) }

    /** Flushes pending save and navigates back. */
    fun onBackClick() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave(_uiState.value)
            _events.send(AddEditEventUiEvent.NavigateBack)
        }
    }

    /** Debounces persist after field changes. */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            performSave(_uiState.value)
        }
    }

    /** Creates or updates the event, then refreshes alarm and widget. */
    private suspend fun performSave(state: AddEditEventUiState) {
        if (state.title.isBlank() && state.description.isBlank() && state.aiChatMessages.isEmpty()) return
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
                .onError { exception, key, args ->
                    _events.send(AddEditEventUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
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
                .onError { exception, key, args ->
                    _events.send(AddEditEventUiEvent.ShowError(errorStrings.resolve(key, args, exception)))
                }
        }
    }

    /** Maps uiState to a [CalendarEvent] for persistence. */
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
        aiChatMessages = state.aiChatMessages,
    )

    /** Subscribes to tag list and prunes stale selected guids. */
    private fun observeTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch {
                    _events.send(
                        AddEditEventUiEvent.ShowError(
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

    /** Subscribes to visible AI actions for the bottom sheet. */
    private fun observeVisibleAiActions() {
        viewModelScope.launch {
            getVisibleAiActionsUseCase()
                .catch {
                    _events.send(
                        AddEditEventUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.LOAD_AI_ACTIONS_FAILED),
                        ),
                    )
                }
                .collect { actions ->
                    _uiState.update { it.copy(visibleAiActions = actions) }
                }
        }
    }
}
