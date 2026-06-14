package ua.danichapps.radiantdays.ui.addevent

/**
 * One-time UI events emitted by [AddEditEventViewModel] and consumed exactly once in the screen.
 *
 * Replaces the `isSaved: Boolean` and `error: String?` fields that lived in UiState вЂ”
 * those are **not** state (they don't survive configuration change meaningfully) but events.
 */
sealed interface AddEditEventUiEvent {
    /** Navigate back after pending changes are flushed. */
    data object NavigateBack : AddEditEventUiEvent

    /** Show an error snack-bar with [message]. */
    data class ShowError(val message: String) : AddEditEventUiEvent

    /** Show an info snack-bar with [message]. */
    data class ShowSnackbar(val message: String) : AddEditEventUiEvent
}
