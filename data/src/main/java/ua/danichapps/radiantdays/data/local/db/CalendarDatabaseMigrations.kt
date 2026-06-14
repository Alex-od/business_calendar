package ua.danichapps.radiantdays.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema migrations for [CalendarDatabase].
 *
 * When [CalendarDatabase.version] is incremented, add a new [Migration] here and export the schema
 * JSON via KSP (`room.schemaLocation`). Never use [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration]:
 * a failed migration must crash the app instead of silently wiping user data.
 */
object CalendarDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE calendar_events ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS folders (
                    guid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    PRIMARY KEY(guid)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS calendar_events_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    start_time_millis INTEGER NOT NULL,
                    end_time_millis INTEGER NOT NULL,
                    is_all_day INTEGER NOT NULL,
                    color TEXT NOT NULL,
                    notification_minutes_before INTEGER NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    folder_guid TEXT,
                    FOREIGN KEY(folder_guid) REFERENCES folders(guid)
                        ON UPDATE CASCADE ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO calendar_events_new (
                    id,
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed,
                    folder_guid
                )
                SELECT
                    id,
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed,
                    NULL
                FROM calendar_events
                """.trimIndent()
            )
            db.execSQL("DROP TABLE calendar_events")
            db.execSQL("ALTER TABLE calendar_events_new RENAME TO calendar_events")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_folder_guid ON calendar_events(folder_guid)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_calendar_events_folder_guid")
            db.execSQL("ALTER TABLE calendar_events RENAME TO notes")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_folder_guid ON notes(folder_guid)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    description TEXT NOT NULL,
                    start_time_millis INTEGER NOT NULL,
                    end_time_millis INTEGER NOT NULL,
                    is_all_day INTEGER NOT NULL,
                    color TEXT NOT NULL,
                    notification_minutes_before INTEGER NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    folder_guid TEXT,
                    FOREIGN KEY(folder_guid) REFERENCES folders(guid)
                        ON UPDATE CASCADE ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO notes_new (
                    id,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed,
                    folder_guid
                )
                SELECT
                    id,
                    CASE
                        WHEN TRIM(description) = '' THEN title
                        ELSE description
                    END,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed,
                    folder_guid
                FROM notes
                """.trimIndent()
            )
            db.execSQL("DROP TABLE notes")
            db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_folder_guid ON notes(folder_guid)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE folders ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN alarm_time_millis INTEGER")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE folders RENAME TO tags")
            db.execSQL("DELETE FROM tags WHERE guid = '__general__'")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS note_tags (
                    note_id INTEGER NOT NULL,
                    tag_guid TEXT NOT NULL,
                    PRIMARY KEY(note_id, tag_guid),
                    FOREIGN KEY(note_id) REFERENCES notes(id)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    FOREIGN KEY(tag_guid) REFERENCES tags(guid)
                        ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tag_guid ON note_tags(tag_guid)")

            db.execSQL(
                """
                INSERT INTO note_tags (note_id, tag_guid)
                SELECT id, folder_guid FROM notes
                WHERE folder_guid IS NOT NULL AND folder_guid != '__general__'
                """.trimIndent(),
            )

            db.execSQL("DROP INDEX IF EXISTS index_notes_folder_guid")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL,
                    start_time_millis INTEGER NOT NULL,
                    end_time_millis INTEGER NOT NULL,
                    is_all_day INTEGER NOT NULL,
                    color TEXT NOT NULL,
                    notification_minutes_before INTEGER NOT NULL,
                    alarm_time_millis INTEGER,
                    is_completed INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO notes_new (
                    id,
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    alarm_time_millis,
                    is_completed
                )
                SELECT
                    id,
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    alarm_time_millis,
                    is_completed
                FROM notes
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE notes")
            db.execSQL("ALTER TABLE notes_new RENAME TO notes")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE tags ADD COLUMN color TEXT NOT NULL DEFAULT 'DEFAULT'",
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE notes ADD COLUMN created_at_millis INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE notes ADD COLUMN updated_at_millis INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                UPDATE notes
                SET created_at_millis = start_time_millis,
                    updated_at_millis = start_time_millis
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_actions (
                    guid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    prompt TEXT NOT NULL,
                    is_visible INTEGER NOT NULL DEFAULT 1,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    is_built_in INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(guid)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ai_actions (guid, name, description, prompt, is_visible, sort_order, is_built_in)
                VALUES (
                    'a1000000-0000-4000-8000-000000000001',
                    'Улучшить текст',
                    'Исправляет орфографию и стиль',
                    'Исправь орфографию и стиль, сохрани смысл:\n{{text}}',
                    1, 0, 1
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ai_actions (guid, name, description, prompt, is_visible, sort_order, is_built_in)
                VALUES (
                    'a1000000-0000-4000-8000-000000000002',
                    'Сократить',
                    'Краткое резюме заметки',
                    'Сократи следующий текст до 1–2 предложений, сохрани смысл:\n{{text}}',
                    1, 1, 1
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ai_actions (guid, name, description, prompt, is_visible, sort_order, is_built_in)
                VALUES (
                    'a1000000-0000-4000-8000-000000000003',
                    'Список задач',
                    'Преобразует текст в чеклист',
                    'Преобразуй следующий текст в маркированный чеклист:\n{{text}}',
                    1, 2, 1
                )
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
    )
}
