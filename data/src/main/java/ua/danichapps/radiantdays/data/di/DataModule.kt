package ua.danichapps.radiantdays.data.di

import androidx.room.Room
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
import ua.danichapps.radiantdays.data.local.db.CalendarDatabaseMigrations
import ua.danichapps.radiantdays.data.remote.api.KtorCalendarDataSource
import ua.danichapps.radiantdays.data.remote.api.RemoteCalendarDataSource
import ua.danichapps.radiantdays.data.repository.AiActionRepositoryImpl
import ua.danichapps.radiantdays.data.repository.CalendarEventRepositoryImpl
import ua.danichapps.radiantdays.data.repository.TagRepositoryImpl
import ua.danichapps.radiantdays.domain.repository.AiActionRepository
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import ua.danichapps.radiantdays.domain.repository.TagRepository

/**
 * Koin module that wires the entire data layer.
 *
 * Exposed bindings:
 * - [CalendarDatabase]: singleton Room database
 * - [CalendarEventRepository]: bound to [CalendarEventRepositoryImpl]
 * - [HttpClient]: Ktor client with JSON + logging
 *
 * Add migrations in [CalendarDatabaseMigrations] when [CalendarDatabase.version] is incremented.
 */
val dataModule = module {

    // Room
    single {
        Room.databaseBuilder(
            androidContext(),
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME,
        ).addMigrations(*CalendarDatabaseMigrations.ALL)
            .build()
    }

    single { get<CalendarDatabase>().calendarEventDao() }
    single { get<CalendarDatabase>().tagDao() }
    single { get<CalendarDatabase>().noteTagDao() }
    single { get<CalendarDatabase>().aiActionDao() }

    // Ktor HTTP client
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

    // Remote data source
    single<RemoteCalendarDataSource> {
        KtorCalendarDataSource(
            client  = get(),
            baseUrl = "https://api.radiantdays.example.com/v1",
        )
    }

    // Repository bindings
    single<CalendarEventRepository> {
        CalendarEventRepositoryImpl(dao = get(), noteTagDao = get())
    }

    single<TagRepository> {
        TagRepositoryImpl(dao = get())
    }

    single<AiActionRepository> {
        AiActionRepositoryImpl(dao = get())
    }
}
