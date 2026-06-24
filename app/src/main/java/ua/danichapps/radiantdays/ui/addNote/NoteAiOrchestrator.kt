package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.locale.DomainErrorStrings

/** AI sheet, chat history, and completion orchestration for the note editor screen. */
internal class NoteAiOrchestrator(
    private val scope: CoroutineScope,
    private val apiKeyStore: AiApiKeyStore,
    private val localeStore: AppLocaleStore,
    private val getVisibleAiActionsUseCase: GetVisibleAiActionsUseCase,
    private val runAiActionUseCase: RunAiActionUseCase,
    private val continueAiChatUseCase: ContinueAiChatUseCase,
    private val errorStrings: DomainErrorStrings,
    private val readState: () -> AddEditNoteUiState,
    private val updateState: ((AddEditNoteUiState) -> AddEditNoteUiState) -> Unit,
    private val onShowError: suspend (String) -> Unit,
    private val onScheduleAutoSave: () -> Unit,
    private val onSyncDescriptionFromFirstChatMessage: (String) -> Unit,
) {
    fun refreshKeyStatus() {
        updateState { it.copy(isAiKeySaved = apiKeyStore.hasKey()) }
    }

    fun observeVisibleActions() {
        scope.launch {
            getVisibleAiActionsUseCase()
                .catch {
                    onShowError(errorStrings.resolve(MessageKey.LOAD_AI_ACTIONS_FAILED))
                }
                .collect { actions ->
                    updateState { it.copy(visibleAiActions = actions) }
                }
        }
    }

    fun onAiButtonClick() {
        val state = readState()
        if (!state.isAiKeySaved || state.description.isBlank()) return
        updateState { it.copy(aiSheetVisible = true) }
    }

    fun openActionsSheet() {
        updateState { it.copy(aiSheetVisible = true) }
    }

    fun dismissActionsSheet() {
        updateState { it.copy(aiSheetVisible = false) }
    }

    fun onActionSelected(actionGuid: String) {
        val state = readState()
        val history = state.aiChatMessages
        updateState { it.copy(aiSheetVisible = false, aiLoading = true) }
        scope.launch {
            runAiActionUseCase(actionGuid, buildNoteContext(state), history)
                .onSuccess { result ->
                    updateState {
                        it.copy(
                            aiLoading = false,
                            aiChatMessages = history +
                                result.userMessage +
                                AiChatMessage(AiChatRole.ASSISTANT, result.response),
                        )
                    }
                    onScheduleAutoSave()
                }
                .onError { exception, key, args ->
                    updateState { it.copy(aiLoading = false) }
                    onShowError(errorStrings.resolve(key, args, exception))
                }
        }
    }

    fun onChatMessageEdit(index: Int, content: String) {
        val messages = readState().aiChatMessages
        if (index !in messages.indices) return
        val current = messages[index]
        if (current.content == content) return
        updateState { state ->
            state.copy(
                aiChatMessages = messages.mapIndexed { i, message ->
                    if (i == index) {
                        message.copy(
                            content = content,
                            apiContent = message.apiContent,
                            actionLabel = message.actionLabel,
                        )
                    } else {
                        message
                    }
                },
            )
        }
        if (index == 0 && current.role == AiChatRole.USER) {
            onSyncDescriptionFromFirstChatMessage(content)
        }
        onScheduleAutoSave()
    }

    fun onChatMessageDelete(index: Int) {
        if (readState().aiChatLoading) return
        val messages = readState().aiChatMessages
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

        updateState { state ->
            state.copy(aiChatMessages = messages.filterIndexed { i, _ -> i !in indicesToRemove })
        }
        onScheduleAutoSave()
    }

    fun onChatSend(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank() || readState().aiChatLoading) return

        val history = readState().aiChatMessages
        val userMessage = AiChatMessage(AiChatRole.USER, trimmed)
        updateState {
            it.copy(
                aiChatMessages = history + userMessage,
                aiChatLoading = true,
            )
        }

        scope.launch {
            continueAiChatUseCase(history, trimmed)
                .onSuccess { response ->
                    updateState { state ->
                        state.copy(
                            aiChatLoading = false,
                            aiChatMessages = state.aiChatMessages +
                                AiChatMessage(AiChatRole.ASSISTANT, response),
                        )
                    }
                    onScheduleAutoSave()
                }
                .onError { exception, key, args ->
                    updateState { state ->
                        state.copy(
                            aiChatLoading = false,
                            aiChatMessages = state.aiChatMessages.dropLast(1),
                        )
                    }
                    onShowError(errorStrings.resolve(key, args, exception))
                }
        }
    }

    private fun buildNoteContext(state: AddEditNoteUiState): AiNoteContext {
        val tagNames = state.tags
            .filter { tag -> tag.guid in state.selectedTagGuids }
            .map { tag -> tag.name }
        return AiNoteContext(
            text = state.description,
            title = state.title,
            tagNames = tagNames,
            noteDateMillis = state.startTimeMillis,
            locale = localeStore.resolveLocale(),
        )
    }
}
