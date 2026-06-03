package ua.danichapps.radiantdays.ui.folders

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
import ua.danichapps.radiantdays.domain.model.Folder
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddFolderUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteFolderUseCase
import ua.danichapps.radiantdays.domain.usecase.GetFoldersUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateFolderUseCase

class FolderSettingsViewModel(
    private val getFoldersUseCase: GetFoldersUseCase,
    private val addFolderUseCase: AddFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderSettingsUiState())
    val uiState: StateFlow<FolderSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<FolderSettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<FolderSettingsUiEvent> = _events.receiveAsFlow()

    init {
        observeFolders()
    }

    fun onFolderNameChange(value: String) {
        _uiState.update { it.copy(folderName = value, folderNameError = null) }
    }

    fun addFolder() {
        val name = _uiState.value.folderName
        viewModelScope.launch {
            addFolderUseCase(name)
                .onSuccess { folder ->
                    _uiState.update { it.copy(folderName = "", folderNameError = null) }
                    _events.send(FolderSettingsUiEvent.FolderCreated(folder.guid))
                }
                .onError { _, message ->
                    _uiState.update { it.copy(folderNameError = message) }
                }
        }
    }

    fun requestEdit(folder: Folder) {
        _uiState.update { it.copy(editingFolder = folder) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editingFolder = null) }
    }

    fun updateFolder(name: String) {
        val folder = _uiState.value.editingFolder ?: return
        viewModelScope.launch {
            updateFolderUseCase(folder.copy(name = name))
                .onSuccess {
                    _uiState.update { it.copy(editingFolder = null) }
                }
                .onError { _, message ->
                    _events.send(FolderSettingsUiEvent.ShowError(message))
                }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            deleteFolderUseCase(folder.guid).onError { _, message ->
                _events.send(FolderSettingsUiEvent.ShowError(message))
            }
        }
    }

    fun toggleFolderPinned(folder: Folder) {
        viewModelScope.launch {
            updateFolderUseCase(folder.copy(isPinned = !folder.isPinned))
                .onError { _, message ->
                    _events.send(FolderSettingsUiEvent.ShowError(message))
                }
        }
    }

    private fun observeFolders() {
        viewModelScope.launch {
            getFoldersUseCase()
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        FolderSettingsUiEvent.ShowError(throwable.message ?: "Не удалось загрузить папки"),
                    )
                }
                .collect { folders ->
                    val foldersWithGeneral = listOf(Folder.general()) + folders
                    _uiState.update { it.copy(isLoading = false, folders = foldersWithGeneral) }
                }
        }
    }
}
