package ua.danichapps.mybusinesscalendar.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.danichapps.mybusinesscalendar.data.local.dao.CalendarEventDao
import ua.danichapps.mybusinesscalendar.data.local.entity.CalendarEventEntity

/**
 * Single Room database for the application.
 *
 * To add a migration, increment [version] and provide a [androidx.room.migration.Migration]
 * object in the [androidx.room.Room.databaseBuilder] call inside [DataModule].
 */
@Database(
    entities  = [CalendarEventEntity::class],
    version   = 1,
    exportSchema = true,
)
abstract class CalendarDatabase : RoomDatabase() {

    /** Exposes the DAO for calendar events. */
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        const val DATABASE_NAME = "calendar.db"
    }
}
