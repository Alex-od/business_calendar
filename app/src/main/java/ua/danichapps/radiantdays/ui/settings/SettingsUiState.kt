package ua.danichapps.radiantdays.ui.settings

data class SettingsUiState(
    val apiKeyInput: String = "",
    val isKeySaved: Boolean = false,
    val statusMessage: String = "AI работает в режиме заглушки",
)
