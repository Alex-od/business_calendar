package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/**
 * Fetches events that start within a given time window.
 *
 * Used by:
 * - [EventNotificationWorker] вЂ” to determine which events need reminders.
 * - [CalendarWidget]          вЂ” to populate the home-screen widget.
 *
 * @param repository Data source abstraction (injected).
 */
class GetUpcomingEventsUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param fromMillis Start of the window (inclusive) in epoch milliseconds.
     * @param toMillis   End of the window (exclusive) in epoch milliseconds.
     */
    suspend operator fun invoke(fromMillis: Long, toMillis: Long): DomainResult<List<CalendarEvent>> =
        repository.getUpcomingEvents(fromMillis, toMillis)
}
