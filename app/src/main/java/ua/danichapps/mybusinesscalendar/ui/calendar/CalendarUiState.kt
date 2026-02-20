package ua.danichapps.mybusinesscalendar.ui.calendar

import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent

/**
 * Immutable snapshot of the calendar screen state.
 *
 * Contains only **persistent** state — data that should survive configuration change.
 * One-shot events (errors, navigation) are emitted via [CalendarViewModel.events] channel.
 *
 * @property currentMonthMillis Epoch ms of any day within the displayed month.
 * @property selectedDayMillis  The tapped day; events list shows events for this day.
 * @property eventsForDay       Live list of events for [selectedDayMillis] (bottom list).
 * @property eventsForMonth     All events of the displayed month grouped by day-midnight
 *                              millis; drives the per-cell event summary in the grid.
 * @property isLoading          `true` while a data fetch is in progress.
 */
data class CalendarUiState(
    val currentMonthMillis: Long = System.currentTimeMillis(),
    val selectedDayMillis: Long  = System.currentTimeMillis(),
    val eventsForDay: List<CalendarEvent> = emptyList(),
    val eventsForMonth: Map<Long, List<CalendarEvent>> = emptyMap(),
    val isLoading: Boolean = false,
)
