package ua.danichapps.radiantdays.ui.folders

sealed interface FolderSettingsUiEvent {
    data class FolderCreated(val folderGuid: String) : FolderSettingsUiEvent
    data class ShowError(val message: String) : FolderSettingsUiEvent
}
