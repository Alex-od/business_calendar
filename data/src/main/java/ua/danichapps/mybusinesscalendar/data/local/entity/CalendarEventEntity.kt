package ua.danichapps.mybusinesscalendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity that maps 1-to-1 to the `calendar_events` table.
 *
 * Intentionally separated from the domain [ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent]
 * to isolate the data layer from the domain layer — any schema change stays
 * inside this file and its mapper.
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "start_time_millis")
    val startTimeMillis: Long,

    @ColumnInfo(name = "end_time_millis")
    val endTimeMillis: Long,

    @ColumnInfo(name = "is_all_day")
    val isAllDay: Boolean,

    /** Stored as the enum name string for readability in raw DB inspection. */
    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "notification_minutes_before")
    val notificationMinutesBefore: Int,
)
