package ua.danichapps.radiantdays.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.danichapps.radiantdays.data.local.dao.CalendarEventDao
import ua.danichapps.radiantdays.data.local.dao.AiActionDao
import ua.danichapps.radiantdays.data.local.dao.NoteTagDao
import ua.danichapps.radiantdays.data.local.dao.TagDao
import ua.danichapps.radiantdays.data.local.entity.AiActionEntity
import ua.danichapps.radiantdays.data.local.entity.NoteEntity
import ua.danichapps.radiantdays.data.local.entity.NoteTagCrossRef
import ua.danichapps.radiantdays.data.local.entity.TagEntity

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, AiActionEntity::class],
    version = 13,
    exportSchema = true,
)
abstract class CalendarDatabase : RoomDatabase() {

    abstract fun calendarEventDao(): CalendarEventDao

    abstract fun tagDao(): TagDao

    abstract fun noteTagDao(): NoteTagDao

    abstract fun aiActionDao(): AiActionDao

    companion object {
        const val DATABASE_NAME = "calendar.db"
    }
}
