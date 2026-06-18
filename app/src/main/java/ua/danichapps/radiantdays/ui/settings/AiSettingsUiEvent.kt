package ua.danichapps.radiantdays.ui.settings

sealed interface AiSettingsUiEvent {
    data class ShowSnackbar(val message: String) : AiSettingsUiEvent
    data class ShowErrorDialog(val title: String, val message: String) : AiSettingsUiEvent
}
