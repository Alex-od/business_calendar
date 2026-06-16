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
import ua.danichapps.radiantdays.locale.AppLocaleManager
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.locale.AppStrings

class SettingsViewModel(
    private val apiKeyStore: AiApiKeyStore,
    private val localeStore: AppLocaleStore,
    private val localeManager: AppLocaleManager,
    private val appStrings: AppStrings,
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
            _events.send(SettingsUiEvent.ShowSnackbar(appStrings.settingsModelChanged()))
        }
    }

    fun onLanguageSelected(tag: String?) {
        if (tag == _uiState.value.selectedLanguageTag) return
        localeStore.saveTag(tag)
        localeManager.apply(tag)
        _uiState.update { it.copy(selectedLanguageTag = localeStore.getTag()) }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.LocaleChanged)
        }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) {
            viewModelScope.launch {
                _events.send(SettingsUiEvent.ShowSnackbar(appStrings.settingsEnterApiKey()))
            }
            return
        }
        apiKeyStore.saveKey(key)
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = true,
                aiStatus = AiConnectionStatus.CONNECTED,
                isApiKeySectionExpanded = false,
            )
        }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.ShowSnackbar(appStrings.settingsKeySaved()))
        }
    }

    fun clearApiKey() {
        apiKeyStore.clearKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = false,
                aiStatus = AiConnectionStatus.STUB,
                isApiKeySectionExpanded = false,
            )
        }
        viewModelScope.launch {
            _events.send(SettingsUiEvent.ShowSnackbar(appStrings.settingsKeyRemoved()))
        }
    }

    private fun refreshStatus() {
        val saved = apiKeyStore.hasKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = saved,
                aiStatus = if (saved) AiConnectionStatus.CONNECTED else AiConnectionStatus.STUB,
                selectedModelId = apiKeyStore.getModelId(),
                selectedLanguageTag = localeStore.getTag(),
            )
        }
    }
}

enum class AiConnectionStatus {
    STUB,
    CONNECTED,
}
