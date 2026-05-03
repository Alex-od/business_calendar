package ua.danichapps.radiantdays.data.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ua.danichapps.radiantdays.data.local.db.CalendarDatabase
import ua.danichapps.radiantdays.data.remote.api.KtorCalendarDataSource
import ua.danichapps.radiantdays.data.remote.api.RemoteCalendarDataSource
import ua.danichapps.radiantdays.data.repository.CalendarEventRepositoryImpl
import ua.danichapps.radiantdays.data.repository.FolderRepositoryImpl
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.repository.FolderRepository

/**
 * Koin module that wires the entire data layer.
 *
 * Exposed bindings:
 * - [CalendarDatabase]       вЂ” singleton Room database
 * - [CalendarEventRepository] вЂ” bound to [CalendarEventRepositoryImpl]
 * - [HttpClient]             вЂ” Ktor client with JSON + logging
 *
 * Add database migrations to the `Room.databaseBuilder` call when [CalendarDatabase.version]
 * is incremented.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calendar_events ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
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

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_calendar_events_folder_guid")
        db.execSQL("ALTER TABLE calendar_events RENAME TO notes")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_folder_guid ON notes(folder_guid)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
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

val dataModule = module {

    // в”Ђв”Ђ Room в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    single {
        Room.databaseBuilder(
            androidContext(),
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    single { get<CalendarDatabase>().calendarEventDao() }
    single { get<CalendarDatabase>().folderDao() }

    // в”Ђв”Ђ Ktor HTTP client в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient         = true
                    }
                )
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
        }
    }

    // в”Ђв”Ђ Remote data source в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    single<RemoteCalendarDataSource> {
        KtorCalendarDataSource(
            client  = get(),
            baseUrl = "https://api.radiantdays.example.com/v1",
        )
    }

    // в”Ђв”Ђ Repository (interface в†’ implementation) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    single<CalendarEventRepository> {
        CalendarEventRepositoryImpl(dao = get())
    }

    single<FolderRepository> {
        FolderRepositoryImpl(dao = get())
    }
}
