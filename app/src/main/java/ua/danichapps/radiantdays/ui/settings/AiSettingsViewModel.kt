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
import ua.danichapps.radiantdays.ai.AiApiKeySanitizer
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.usecase.ValidateAiApiKeyUseCase
import ua.danichapps.radiantdays.locale.AppStrings
import ua.danichapps.radiantdays.locale.DomainErrorStrings

class AiSettingsViewModel(
    private val apiKeyStore: AiApiKeyStore,
    private val appStrings: AppStrings,
    private val validateAiApiKeyUseCase: ValidateAiApiKeyUseCase,
    private val errorStrings: DomainErrorStrings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AiSettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<AiSettingsUiEvent> = _events.receiveAsFlow()

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
            _events.send(AiSettingsUiEvent.ShowSnackbar(appStrings.settingsModelChanged()))
        }
    }

    fun saveApiKey() {
        val key = AiApiKeySanitizer.sanitize(_uiState.value.apiKeyInput)
        if (key.isBlank()) {
            viewModelScope.launch {
                _events.send(AiSettingsUiEvent.ShowSnackbar(appStrings.settingsEnterApiKey()))
            }
            return
        }
        if (_uiState.value.isValidatingApiKey) return

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingApiKey = true) }
            val modelId = _uiState.value.selectedModelId
            when (val result = validateAiApiKeyUseCase(key, modelId)) {
                is DomainResult.Success -> {
                    apiKeyStore.saveKey(key)
                    _uiState.update {
                        it.copy(
                            apiKeyInput = "",
                            isKeySaved = true,
                            isApiKeySectionExpanded = false,
                            isValidatingApiKey = false,
                        )
                    }
                    _events.send(AiSettingsUiEvent.ShowSnackbar(appStrings.settingsKeySaved()))
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isValidatingApiKey = false) }
                    _events.send(
                        AiSettingsUiEvent.ShowErrorDialog(
                            title = appStrings.settingsApiCheckFailedTitle(),
                            message = errorStrings.resolve(
                                result.messageKey,
                                result.messageArgs,
                                result.exception,
                            ),
                        ),
                    )
                }
            }
        }
    }

    fun clearApiKey() {
        apiKeyStore.clearKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = false,
                isApiKeySectionExpanded = false,
                isValidatingApiKey = false,
            )
        }
        viewModelScope.launch {
            _events.send(AiSettingsUiEvent.ShowSnackbar(appStrings.settingsKeyRemoved()))
        }
    }

    private fun refreshStatus() {
        val saved = apiKeyStore.hasKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isKeySaved = saved,
                selectedModelId = apiKeyStore.getModelId(),
                isValidatingApiKey = false,
            )
        }
    }
}
