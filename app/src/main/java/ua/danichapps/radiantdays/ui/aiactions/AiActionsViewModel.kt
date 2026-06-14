package ua.danichapps.radiantdays.ui.aiactions

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
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.GetAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.ReorderAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateAiActionUseCase

class AiActionsViewModel(
    private val getAiActionsUseCase: GetAiActionsUseCase,
    private val addAiActionUseCase: AddAiActionUseCase,
    private val updateAiActionUseCase: UpdateAiActionUseCase,
    private val deleteAiActionUseCase: DeleteAiActionUseCase,
    private val reorderAiActionsUseCase: ReorderAiActionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiActionsUiState())
    val uiState: StateFlow<AiActionsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AiActionsUiEvent>(Channel.BUFFERED)
    val events: Flow<AiActionsUiEvent> = _events.receiveAsFlow()

    init {
        observeActions()
    }

    fun clearAddError() {
        _uiState.update { it.copy(actionNameError = null) }
    }

    fun addAction(name: String, description: String?, prompt: String, isVisible: Boolean) {
        viewModelScope.launch {
            addAiActionUseCase(name, description, prompt, isVisible)
                .onSuccess {
                    _uiState.update { it.copy(actionNameError = null) }
                    _events.send(AiActionsUiEvent.ActionSaved)
                }
                .onError { _, message ->
                    if (isFieldLevelError(message)) {
                        _uiState.update { it.copy(actionNameError = message) }
                    } else {
                        _events.send(AiActionsUiEvent.ShowError(message))
                    }
                }
        }
    }

    fun requestEdit(action: AiAction) {
        _uiState.update { it.copy(editingAction = action) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editingAction = null) }
    }

    fun updateAction(name: String, description: String?, prompt: String, isVisible: Boolean) {
        val action = _uiState.value.editingAction ?: return
        viewModelScope.launch {
            updateAiActionUseCase(
                action.copy(
                    name = name,
                    description = description,
                    prompt = prompt,
                    isVisible = isVisible,
                ),
            ).onSuccess {
                _uiState.update { it.copy(editingAction = null) }
                _events.send(AiActionsUiEvent.ActionSaved)
            }.onError { _, message ->
                _events.send(AiActionsUiEvent.ShowError(message))
            }
        }
    }

    fun deleteAction(action: AiAction) {
        viewModelScope.launch {
            deleteAiActionUseCase(action.guid).onError { _, message ->
                _events.send(AiActionsUiEvent.ShowError(message))
            }
        }
    }

    fun toggleVisible(action: AiAction) {
        viewModelScope.launch {
            updateAiActionUseCase(action.copy(isVisible = !action.isVisible))
                .onError { _, message ->
                    _events.send(AiActionsUiEvent.ShowError(message))
                }
        }
    }

    fun reorderActions(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.actions.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        _uiState.update { it.copy(actions = current) }
        viewModelScope.launch {
            reorderAiActionsUseCase(current.map { action -> action.guid })
                .onError { _, message ->
                    _events.send(AiActionsUiEvent.ShowError(message))
                }
        }
    }

    private fun observeActions() {
        viewModelScope.launch {
            getAiActionsUseCase()
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        AiActionsUiEvent.ShowError(
                            throwable.message ?: "Не удалось загрузить AI-действия",
                        ),
                    )
                }
                .collect { actions ->
                    _uiState.update { it.copy(isLoading = false, actions = actions) }
                }
        }
    }

    private fun isFieldLevelError(message: String): Boolean =
        message == "Введите название действия" ||
            message == "Введите промпт" ||
            message == "Действие с таким названием уже существует"
}
