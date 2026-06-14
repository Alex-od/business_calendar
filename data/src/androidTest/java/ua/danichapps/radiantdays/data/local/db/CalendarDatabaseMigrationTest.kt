package ua.danichapps.radiantdays.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CalendarDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate2To7_matchesExportedSchema() {
        helper.createDatabase(TEST_DB, 2).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            *CalendarDatabaseMigrations.ALL,
        ).close()
    }

    @Test
    fun migrate6To7_preservesExistingRows() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO folders (guid, name, is_pinned)
                VALUES ('folder-1', 'Work', 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO notes (
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed,
                    folder_guid
                ) VALUES (
                    'Standup',
                    1,
                    2,
                    0,
                    'BLUE',
                    15,
                    0,
                    'folder-1'
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            CalendarDatabaseMigrations.MIGRATION_6_7,
        )

        db.query("SELECT COUNT(*) FROM notes").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        db.query("SELECT description, alarm_time_millis FROM notes WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Standup", cursor.getString(0))
            assertNull(if (cursor.isNull(1)) null else cursor.getLong(1))
        }

        db.close()
    }

    @Test
    fun migrate8To9_movesFolderGuidToNoteTags() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                """
                INSERT INTO folders (guid, name, is_pinned)
                VALUES ('tag-work', 'Work', 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO notes (
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    alarm_time_millis,
                    is_completed,
                    folder_guid
                ) VALUES (
                    'Title',
                    'Body',
                    1,
                    2,
                    0,
                    'BLUE',
                    0,
                    NULL,
                    0,
                    'tag-work'
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            CalendarDatabaseMigrations.MIGRATION_8_9,
        )

        db.query("SELECT name FROM tags WHERE guid = 'tag-work'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Work", cursor.getString(0))
        }

        db.query("SELECT tag_guid FROM note_tags WHERE note_id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("tag-work", cursor.getString(0))
        }

        db.query("PRAGMA table_info(notes)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
            assertEquals(false, columns.contains("folder_guid"))
        }

        db.close()
    }

    @Test
    fun migrate9To10_addsTagColorColumn() {
        helper.createDatabase(TEST_DB, 9).apply {
            execSQL(
                """
                INSERT INTO tags (guid, name, is_pinned)
                VALUES ('tag-1', 'Work', 0)
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            CalendarDatabaseMigrations.MIGRATION_9_10,
        )

        db.query("SELECT color FROM tags WHERE guid = 'tag-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("DEFAULT", cursor.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate10To11_backfillsNoteTimestampsFromStartTime() {
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                """
                INSERT INTO notes (
                    title,
                    description,
                    start_time_millis,
                    end_time_millis,
                    is_all_day,
                    color,
                    notification_minutes_before,
                    is_completed
                ) VALUES (
                    'Title',
                    'Body',
                    42,
                    43,
                    0,
                    'DEFAULT',
                    0,
                    0
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            CalendarDatabaseMigrations.MIGRATION_10_11,
        )

        db.query(
            "SELECT created_at_millis, updated_at_millis FROM notes WHERE id = 1",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(42L, cursor.getLong(0))
            assertEquals(42L, cursor.getLong(1))
        }

        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
