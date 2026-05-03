package ua.danichapps.radiantdays.ui.folders

import ua.danichapps.radiantdays.domain.model.Folder

data class FolderSettingsUiState(
    val isLoading: Boolean = true,
    val folderName: String = "",
    val folderNameError: String? = null,
    val folders: List<Folder> = emptyList(),
    val editingFolder: Folder? = null,
)
