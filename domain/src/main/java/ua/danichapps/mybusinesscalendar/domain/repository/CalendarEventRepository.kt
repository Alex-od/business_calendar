package ua.danichapps.mybusinesscalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult

/**
 * Contract for calendar event persistence.
 *
 * The interface lives in the **domain** layer so that business logic and use-cases
 * are completely decoupled from the underlying storage technology (Room, API, etc.).
 *
 * KMP note: this interface uses only Kotlin/stdlib and kotlinx-coroutines types,
 * making it safe to share in a common multiplatform module.
 */
interface CalendarEventRepository {

    /**
     * Returns a cold [Flow] of events whose [CalendarEvent.startTimeMillis] falls
     * within [[dayStartMillis], [dayEndMillis]).
     *
     * The flow re-emits automatically whenever the underlying data changes
     * (reactive / observable query).
     */
    fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEvent>>

    /** Returns a cold [Flow] of all events, ordered by start time ascending. */
    fun getAllEvents(): Flow<List<CalendarEvent>>

    /** Fetches a single event by its [id], or `null` if not found. */
    suspend fun getEventById(id: Long): CalendarEvent?

    /**
     * Persists a new event and returns its auto-generated [Long] ID on success.
     */
    suspend fun addEvent(event: CalendarEvent): DomainResult<Long>

    /** Updates an existing event. [event.id] must be non-zero. */
    suspend fun updateEvent(event: CalendarEvent): DomainResult<Unit>

    /** Deletes the event identified by [id]. */
    suspend fun deleteEvent(id: Long): DomainResult<Unit>

    /**
     * Returns events starting within [[fromMillis], [toMillis]).
     * Intended for notification scheduling and widget data.
     */
    suspend fun getUpcomingEvents(fromMillis: Long, toMillis: Long): DomainResult<List<CalendarEvent>>
}
