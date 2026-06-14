package ua.danichapps.radiantdays.ui.tags

import ua.danichapps.radiantdays.domain.model.Tag

data class TagSettingsUiState(
    val isLoading: Boolean = true,
    val tagNameError: String? = null,
    val tags: List<Tag> = emptyList(),
    val editingTag: Tag? = null,
)
