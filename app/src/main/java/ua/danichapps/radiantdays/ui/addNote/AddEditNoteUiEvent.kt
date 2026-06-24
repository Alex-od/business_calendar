package ua.danichapps.radiantdays.ui.addNote

import ua.danichapps.radiantdays.domain.model.MessageKey

/**
 * One-time UI events emitted by [AddEditNoteViewModel] and consumed exactly once in the screen.
 *
 * Replaces the `isSaved: Boolean` and `error: String?` fields that lived in UiState —
 * those are **not** state (they don't survive configuration change meaningfully) but events.
 */
sealed interface AddEditNoteUiEvent {
    /** Navigate back after pending changes are flushed. */
    data object NavigateBack : AddEditNoteUiEvent

    /** Show an error snack-bar; resolved in the UI layer via [ua.danichapps.radiantdays.locale.DomainErrorStrings]. */
    data class ShowError(
        val key: MessageKey,
        val args: List<String> = emptyList(),
        val cause: Throwable? = null,
    ) : AddEditNoteUiEvent

    /** Show an info snack-bar with [message]. */
    data class ShowSnackbar(val message: String) : AddEditNoteUiEvent
}
