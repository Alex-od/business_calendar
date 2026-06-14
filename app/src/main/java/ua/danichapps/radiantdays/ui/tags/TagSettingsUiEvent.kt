package ua.danichapps.radiantdays.ui.tags

sealed interface TagSettingsUiEvent {
    data class TagCreated(val tagGuid: String) : TagSettingsUiEvent
    data class ShowError(val message: String) : TagSettingsUiEvent
}
