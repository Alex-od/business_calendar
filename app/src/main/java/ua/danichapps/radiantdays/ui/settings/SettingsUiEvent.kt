package ua.danichapps.radiantdays.ui.settings

sealed interface SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent
    data object LocaleChanged : SettingsUiEvent
}
