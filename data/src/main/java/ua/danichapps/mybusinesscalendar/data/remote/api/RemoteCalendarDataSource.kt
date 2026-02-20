package ua.danichapps.mybusinesscalendar.data.remote.api

import ua.danichapps.mybusinesscalendar.data.remote.dto.CalendarEventDto

/**
 * Contract for remote calendar data operations.
 *
 * Abstracting the network layer behind this interface means:
 * - The repository is not coupled to Ktor (or any HTTP client).
 * - Tests can inject a fake implementation without starting a real server.
 * - On KMP, a different engine can be injected per platform.
 */
interface RemoteCalendarDataSource {

    /** Fetches all events from the remote server. */
    suspend fun fetchEvents(): List<CalendarEventDto>

    /** Pushes a new event to the server and returns the server-assigned DTO. */
    suspend fun createEvent(dto: CalendarEventDto): CalendarEventDto

    /** Updates an existing event on the server. */
    suspend fun updateEvent(dto: CalendarEventDto): CalendarEventDto

    /** Deletes the event identified by [id] from the server. */
    suspend fun deleteEvent(id: Long)
}
