package ua.danichapps.radiantdays.ui.addNote

/**
 * One-time UI events emitted by [AddEditNoteViewModel] and consumed exactly once in the screen.
 *
 * Replaces the `isSaved: Boolean` and `error: String?` fields that lived in UiState вЂ”
 * those are **not** state (they don't survive configuration change meaningfully) but events.
 */
sealed interface AddEditNoteUiEvent {
    /** Navigate back after pending changes are flushed. */
    data object NavigateBack : AddEditNoteUiEvent

    /** Show an error snack-bar with [message]. */
    data class ShowError(val message: String) : AddEditNoteUiEvent

    /** Show an info snack-bar with [message]. */
    data class ShowSnackbar(val message: String) : AddEditNoteUiEvent
}
