package ua.danichapps.mybusinesscalendar.data.di

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
import ua.danichapps.mybusinesscalendar.data.local.db.CalendarDatabase
import ua.danichapps.mybusinesscalendar.data.remote.api.KtorCalendarDataSource
import ua.danichapps.mybusinesscalendar.data.remote.api.RemoteCalendarDataSource
import ua.danichapps.mybusinesscalendar.data.repository.CalendarEventRepositoryImpl
import ua.danichapps.mybusinesscalendar.domain.repository.CalendarEventRepository

/**
 * Koin module that wires the entire data layer.
 *
 * Exposed bindings:
 * - [CalendarDatabase]       — singleton Room database
 * - [CalendarEventRepository] — bound to [CalendarEventRepositoryImpl]
 * - [HttpClient]             — Ktor client with JSON + logging
 *
 * Add database migrations to the `Room.databaseBuilder` call when [CalendarDatabase.version]
 * is incremented.
 */
val dataModule = module {

    // ── Room ──────────────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    single { get<CalendarDatabase>().calendarEventDao() }

    // ── Ktor HTTP client ──────────────────────────────────────────────────────
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

    // ── Remote data source ────────────────────────────────────────────────────
    single<RemoteCalendarDataSource> {
        KtorCalendarDataSource(
            client  = get(),
            baseUrl = "https://api.mybusinesscalendar.example.com/v1",
        )
    }

    // ── Repository (interface → implementation) ───────────────────────────────
    single<CalendarEventRepository> {
        CalendarEventRepositoryImpl(dao = get())
    }
}
