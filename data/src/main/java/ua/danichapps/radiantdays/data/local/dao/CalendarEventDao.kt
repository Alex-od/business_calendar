package ua.danichapps.radiantdays.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.data.local.entity.NoteEntity
import ua.danichapps.radiantdays.data.local.entity.NoteWithTags

@Dao
interface CalendarEventDao {

    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE start_time_millis >= :dayStart AND start_time_millis < :dayEnd
        ORDER BY start_time_millis ASC
        """
    )
    fun getEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes ORDER BY start_time_millis ASC")
    fun getAllEvents(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM notes n
        WHERE NOT EXISTS (SELECT 1 FROM note_tags nt WHERE nt.note_id = n.id)
        ORDER BY n.updated_at_millis DESC
        """
    )
    fun getEventsWithoutTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT n.* FROM notes n
        INNER JOIN note_tags nt ON n.id = nt.note_id AND nt.tag_guid = :tagGuid
        ORDER BY n.updated_at_millis DESC
        """
    )
    fun getEventsByTag(tagGuid: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): NoteWithTags?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: NoteEntity): Long

    @Update
    suspend fun updateEvent(event: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteEventById(id: Long): Int

    @Query(
        """
        SELECT * FROM notes
        WHERE alarm_time_millis IS NOT NULL
            AND is_completed = 0
            AND (alarm_time_millis - (notification_minutes_before * 60000)) >= :fromMillis
            AND (alarm_time_millis - (notification_minutes_before * 60000)) < :toMillis
        ORDER BY alarm_time_millis ASC
        """
    )
    suspend fun getUpcomingEvents(fromMillis: Long, toMillis: Long): List<NoteEntity>

    @Query(
        """
        SELECT * FROM notes
        WHERE alarm_time_millis IS NOT NULL
            AND is_completed = 0
            AND (alarm_time_millis - (notification_minutes_before * 60000)) > :fromMillis
        ORDER BY alarm_time_millis ASC
        """
    )
    suspend fun getPendingReminders(fromMillis: Long): List<NoteEntity>
}
