package ua.danichapps.radiantdays.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult

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

    /** Notes that have no tags assigned. */
    fun getEventsWithoutTags(): Flow<List<CalendarEvent>>

    /** Notes that include the tag identified by [tagGuid]. */
    fun getEventsByTagGuid(tagGuid: String): Flow<List<CalendarEvent>>

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
     * Returns notes whose reminder fire time falls within [[fromMillis], [toMillis]).
     * Fire time = alarmTimeMillis - notificationMinutesBefore.
     */
    suspend fun getUpcomingEvents(fromMillis: Long, toMillis: Long): DomainResult<List<CalendarEvent>>

    /** Notes with a future reminder fire time, not completed. */
    suspend fun getPendingReminders(fromMillis: Long): DomainResult<List<CalendarEvent>>
}
