package ua.danichapps.mybusinesscalendar.ui.calendar

/**
 * One-time UI events emitted by [CalendarViewModel] and consumed exactly once in the screen.
 *
 * Using a [kotlinx.coroutines.channels.Channel] instead of a `String?` field in UiState
 * avoids the classic problem of re-triggering the same snack-bar after configuration change.
 */
sealed interface CalendarUiEvent {
    /** Show an error snack-bar with [message]. */
    data class ShowError(val message: String) : CalendarUiEvent
}
