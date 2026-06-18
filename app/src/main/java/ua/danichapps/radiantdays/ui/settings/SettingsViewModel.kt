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
import ua.danichapps.radiantdays.ai.AiModels
import ua.danichapps.radiantdays.locale.AppLocaleManager
import ua.danichapps.radiantdays.locale.AppLocaleStore

class SettingsViewModel(
    private val apiKeyStore: AiApiKeyStore,
    private val localeStore: AppLocaleStore,
    private val localeManager: AppLocaleManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<SettingsUiEvent> = _events.receiveAsFlow()

    init {
        refreshLanguage()
        refreshAiSummary()
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

    fun refreshAiSummary() {
        val modelDisplayName = if (apiKeyStore.hasKey()) {
            AiModels.findById(apiKeyStore.getModelId())?.displayName
        } else {
            null
        }
        _uiState.update { it.copy(aiModelDisplayName = modelDisplayName) }
    }

    private fun refreshLanguage() {
        _uiState.update { it.copy(selectedLanguageTag = localeStore.getTag()) }
    }
}
