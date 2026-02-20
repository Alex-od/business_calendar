package ua.danichapps.mybusinesscalendar.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ua.danichapps.mybusinesscalendar.data.remote.dto.CalendarEventDto

/**
 * Ktor-based implementation of [RemoteCalendarDataSource].
 *
 * The [baseUrl] and [HttpClient] are injected through Koin, making it easy to
 * swap the client engine (e.g. OkHttp for Android, Darwin for iOS in KMP).
 *
 * @param client  Ktor [HttpClient] configured with JSON serialization.
 * @param baseUrl Base URL of the REST API (e.g. `"https://api.example.com/v1"`).
 */
class KtorCalendarDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) : RemoteCalendarDataSource {

    override suspend fun fetchEvents(): List<CalendarEventDto> =
        client.get("$baseUrl/events").body()

    override suspend fun createEvent(dto: CalendarEventDto): CalendarEventDto =
        client.post("$baseUrl/events") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    override suspend fun updateEvent(dto: CalendarEventDto): CalendarEventDto =
        client.put("$baseUrl/events/${dto.id}") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    override suspend fun deleteEvent(id: Long) {
        client.delete("$baseUrl/events/$id")
    }
}
