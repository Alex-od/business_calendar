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
        observeTags()
        observeVisibleAiActions()
    }

    fun refreshAiKeyStatus() {
        _uiState.update { it.copy(isAiKeySaved = apiKeyStore.hasKey()) }
    }

    fun onAiButtonClick() {
        if (!_uiState.value.isAiKeySaved) return
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
                locale = localeStore.resolveLocale(),
            )
            runAiActionUseCase(actionGuid, context)
                .onSuccess { result ->
                    val actionName = state.visibleAiActions
                        .firstOrNull { action -> action.guid == actionGuid }
                        ?.name
                    _uiState.update {
                        it.copy(
                            aiLoading = false,
                            aiResultText = result.response,
                            aiChatMessages = listOf(
                                AiChatMessage(
                                    role = AiChatRole.USER,
                                    content = state.description,
                                    apiContent = result.resolvedPrompt,
                                    actionLabel = actionName,
                                ),
                                AiChatMessage(AiChatRole.ASSISTANT, result.response),
                            ),
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

    fun onAiResultDismiss() {
        _uiState.update { it.copy(aiResultText = null, aiChatMessages = emptyList()) }
        scheduleAutoSave()
    }

    fun onAiResultContinueChat() {
        if (_uiState.value.aiChatMessages.isEmpty()) return
        _uiState.update { it.copy(aiResultText = null) }
        scheduleAutoSave()
    }

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
        scheduleAutoSave()
    }

    fun onAiChatMessageDelete(index: Int) {
        if (_uiState.value.aiChatLoading) return
        val messages = _uiState.value.aiChatMessages
        if (index !in messages.indices) return
        _uiState.update { state ->
            state.copy(aiChatMessages = messages.filterIndexed { i, _ -> i != index })
        }
        scheduleAutoSave()
    }

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

    fun onAiChatReplace() {
        val result = latestAssistantMessage() ?: return
        applyDiscreteDescriptionChange(result)
    }

    fun onAiChatAppend() {
        val result = latestAssistantMessage() ?: return
        val current = _uiState.value.description
        val separator = when {
            current.isBlank() -> ""
            current.endsWith(' ') || current.endsWith('\n') -> ""
            else -> " "
        }
        applyDiscreteDescriptionChange(current + separator + result)
    }

    private fun latestAssistantMessage(): String? =
        _uiState.value.aiChatMessages.lastOrNull { it.role == AiChatRole.ASSISTANT }?.content

    fun onAiResultReplace() {
        val result = _uiState.value.aiResultText ?: return
        applyDiscreteDescriptionChange(result)
        _uiState.update { it.copy(aiResultText = null) }
        scheduleAutoSave()
    }

    fun onAiResultAppend() {
        val result = _uiState.value.aiResultText ?: return
        val current = _uiState.value.description
        val separator = when {
            current.isBlank() -> ""
            current.endsWith(' ') || current.endsWith('\n') -> ""
            else -> " "
        }
        applyDiscreteDescriptionChange(current + separator + result)
        _uiState.update { it.copy(aiResultText = null) }
        scheduleAutoSave()
    }

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

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
        scheduleAutoSave()
    }

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

    fun onDescriptionChangeFromVoice(value: String) {
        applyDiscreteDescriptionChange(value)
    }

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

    private fun setDescription(value: String) {
        _uiState.update { it.copy(description = value, descriptionError = null) }
        scheduleAutoSave()
    }

    private fun applyDiscreteDescriptionChange(value: String) {
        if (value == _uiState.value.description) return
        flushUndoGroup()
        pushUndoSnapshot(_uiState.value.description)
        descriptionUndoGroupBase = null
        setDescription(value)
    }

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

    private fun pushUndoSnapshot(text: String) {
        if (descriptionUndoStack.lastOrNull() == text) return
        if (descriptionUndoStack.size >= DESCRIPTION_UNDO_LIMIT) {
            descriptionUndoStack.removeFirst()
        }
        descriptionUndoStack.addLast(text)
        updateCanUndoDescription()
    }

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

    private fun clearDescriptionUndo() {
        descriptionUndoGroupJob?.cancel()
        descriptionUndoGroupJob = null
        descriptionUndoGroupBase = null
        descriptionUndoStack.clear()
        updateCanUndoDescription()
    }

    private fun updateCanUndoDescription() {
        val canUndo = descriptionUndoStack.isNotEmpty() || descriptionUndoGroupBase != null
        if (_uiState.value.canUndoDescription != canUndo) {
            _uiState.update { it.copy(canUndoDescription = canUndo) }
        }
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
