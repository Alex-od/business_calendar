package ua.danichapps.radiantdays.ui.tagnotes

sealed interface TagNotesUiEvent {
    data class ShowError(val message: String) : TagNotesUiEvent
}
