package ua.danichapps.mybusinesscalendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.danichapps.mybusinesscalendar.data.local.entity.CalendarEventEntity

/**
 * Room DAO for calendar event CRUD operations.
 *
 * Reactive queries return [Flow] so the UI layer automatically receives updates
 * without explicit refresh calls.
 */
@Dao
interface CalendarEventDao {

    /**
     * Returns a live [Flow] of events whose [CalendarEventEntity.startTimeMillis]
     * falls within [[dayStart], [dayEnd]).
     */
    @Query(
        """
        SELECT * FROM calendar_events
        WHERE start_time_millis >= :dayStart AND start_time_millis < :dayEnd
        ORDER BY start_time_millis ASC
        """
    )
    fun getEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<CalendarEventEntity>>

    /** Returns a live [Flow] of all events, ordered by start time. */
    @Query("SELECT * FROM calendar_events ORDER BY start_time_millis ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    /** Returns a single event by its [id], or `null` if not found. */
    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): CalendarEventEntity?

    /**
     * Inserts a new event and returns its auto-generated row ID.
     * [OnConflictStrategy.ABORT] ensures duplicate IDs are rejected.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    /** Updates an existing event row. Returns the number of rows affected. */
    @Update
    suspend fun updateEvent(event: CalendarEventEntity): Int

    /** Deletes the event with the given [id]. Returns the number of rows deleted. */
    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long): Int

    /**
     * Returns all events starting within [[fromMillis], [toMillis]).
     * Used for notification scheduling and widget data.
     */
    @Query(
        """
        SELECT * FROM calendar_events
        WHERE start_time_millis >= :fromMillis AND start_time_millis < :toMillis
        ORDER BY start_time_millis ASC
        """
    )
    suspend fun getUpcomingEvents(fromMillis: Long, toMillis: Long): List<CalendarEventEntity>
}
