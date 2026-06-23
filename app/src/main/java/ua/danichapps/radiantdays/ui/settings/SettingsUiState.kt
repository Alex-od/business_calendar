package ua.danichapps.radiantdays.ui.settings

import ua.danichapps.radiantdays.ui.theme.AppThemeMode

data class SettingsUiState(
    val selectedLanguageTag: String? = null,
    val selectedThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val aiModelDisplayName: String? = null,
)
