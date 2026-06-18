package ua.danichapps.radiantdays.ui.settings

import ua.danichapps.radiantdays.ai.AiModelOption
import ua.danichapps.radiantdays.ai.AiModels

data class AiSettingsUiState(
    val apiKeyInput: String = "",
    val isKeySaved: Boolean = false,
    val isApiKeySectionExpanded: Boolean = false,
    val isValidatingApiKey: Boolean = false,
    val selectedModelId: String = AiModels.DEFAULT_ID,
    val availableModels: List<AiModelOption> = AiModels.options,
)
