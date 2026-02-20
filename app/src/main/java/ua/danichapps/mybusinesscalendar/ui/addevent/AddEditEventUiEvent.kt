package ua.danichapps.mybusinesscalendar.ui.addevent

/**
 * One-time UI events emitted by [AddEditEventViewModel] and consumed exactly once in the screen.
 *
 * Replaces the `isSaved: Boolean` and `error: String?` fields that lived in UiState —
 * those are **not** state (they don't survive configuration change meaningfully) but events.
 */
sealed interface AddEditEventUiEvent {
    /** Navigate back after a successful save. */
    data object NavigateBack : AddEditEventUiEvent

    /** Show an error snack-bar with [message]. */
    data class ShowError(val message: String) : AddEditEventUiEvent
}
