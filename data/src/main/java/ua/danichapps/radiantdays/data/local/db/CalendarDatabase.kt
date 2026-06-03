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
 * To add a migration, increment [version], add a [Migration] in [CalendarDatabaseMigrations],
 * and export the schema JSON via KSP.
 */
@Database(
    entities  = [NoteEntity::class, FolderEntity::class],
    version   = 7,
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
