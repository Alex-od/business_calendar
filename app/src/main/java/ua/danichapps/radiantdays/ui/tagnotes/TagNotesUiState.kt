package ua.danichapps.radiantdays.ui.tagnotes

import ua.danichapps.radiantdays.domain.model.CalendarEvent

data class TagNotesUiState(
    val tagName: String = "",
    val isUntaggedFilter: Boolean = false,
    val isLoading: Boolean = true,
    val notes: List<CalendarEvent> = emptyList(),
)
