package ua.danichapps.radiantdays.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.danichapps.radiantdays.data.local.dao.CalendarEventDao
import ua.danichapps.radiantdays.data.local.dao.FolderDao
import ua.danichapps.radiantdays.data.local.entity.FolderEntity
import ua.danichapps.radiantdays.data.local.entity.NoteEntity

/**
 * Single Room database for the application.
 *
 * To add a migration, increment [version] and provide a [androidx.room.migration.Migration]
 * object in the [androidx.room.Room.databaseBuilder] call inside [DataModule].
 */
@Database(
    entities  = [NoteEntity::class, FolderEntity::class],
    version   = 5,
    exportSchema = true,
)
abstract class CalendarDatabase : RoomDatabase() {

    /** Exposes the DAO for calendar events. */
    abstract fun calendarEventDao(): CalendarEventDao

    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "calendar.db"
    }
}
