package ua.danichapps.radiantdays.ui.settings

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
import ua.danichapps.radiantdays.ai.AiApiKeyStore

class SettingsViewModel(
    private val apiKeyStore: AiApiKeyStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<SettingsUiEvent> = _events.receiveAsFlow()

    init {
        refreshStatus()
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKeyInput = value) }
    }

    fun onToggleApiKeySection() {
        _uiState.update { it.copy(isApiKeySectionExpanded = !it.isApiKeySectionExpanded) }
    }

    fun onModelSelected(id: String) {
        if (id == _uiState.value.selectedModelId) return
        apiKeyStore.saveModelId(id)
        _uiState.update { it.copy(selectedModelId = apiKeyStore.getModelId()) }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.ShowSnackbar("Модель изменена"))
        }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) {
            viewModelScope.launch {
                _events.send(SettingsUiEvent.ShowSnackbar("Введите API-ключ"))
            }
            return
        }
        apiKeyStore.saveKey(key)
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = true,
                statusMessage = "OpenAI подключён",
                isApiKeySectionExpanded = false,
            )
        }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.ShowSnackbar("Ключ сохранён"))
        }
    }

    fun clearApiKey() {
        apiKeyStore.clearKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = false,
                statusMessage = "AI работает в режиме заглушки",
                isApiKeySectionExpanded = false,
            )
        }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.ShowSnackbar("Ключ удалён"))
        }
    }

    private fun refreshStatus() {
        val saved = apiKeyStore.hasKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = saved,
                statusMessage = if (saved) "OpenAI подключён" else "AI работает в режиме заглушки",
                selectedModelId = apiKeyStore.getModelId(),
            )
        }
    }
}
