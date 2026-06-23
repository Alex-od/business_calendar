package ua.danichapps.radiantdays.ui.settings

sealed interface SettingsUiEvent {
    data object LocaleChanged : SettingsUiEvent
}
