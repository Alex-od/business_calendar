package ua.danichapps.radiantdays.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity that maps 1-to-1 to the `notes` table.
 *
 * Intentionally separated from the domain [ua.danichapps.radiantdays.domain.model.CalendarEvent]
 * to isolate the data layer from the domain layer. Any schema change stays
 * inside this file and its mapper.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["guid"],
            childColumns = ["folder_guid"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["folder_guid"])],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

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

    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "folder_guid")
    val folderGuid: String? = null,
)
