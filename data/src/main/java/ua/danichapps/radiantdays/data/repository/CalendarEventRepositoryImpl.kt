package ua.danichapps.radiantdays.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.danichapps.radiantdays.data.local.dao.CalendarEventDao
import ua.danichapps.radiantdays.data.local.mapper.toDomain
import ua.danichapps.radiantdays.data.local.mapper.toEntity
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/**
 * Concrete implementation of [CalendarEventRepository] backed by Room.
 *
 * Strategy:
 * - **Reads** are served from the local Room database (offline-first).
 * - **Writes** go to Room; network sync can be added here later without
 *   changing the interface or any use-case.
 *
 * All database exceptions are caught and wrapped in [DomainResult.Error] so that
 * the domain and presentation layers never deal with raw `SQLiteException`.
 *
 * @param dao Local Room DAO (injected).
 */
class CalendarEventRepositoryImpl(
    private val dao: CalendarEventDao,
) : CalendarEventRepository {

    override fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEvent>> =
        dao.getEventsForDay(dayStartMillis, dayEndMillis)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getAllEvents(): Flow<List<CalendarEvent>> =
        dao.getAllEvents().map { entities -> entities.map { it.toDomain() } }

    override fun getEventsInGeneralFolder(): Flow<List<CalendarEvent>> =
        dao.getEventsInGeneralFolder().map { entities -> entities.map { it.toDomain() } }

    override fun getEventsByFolderGuid(folderGuid: String): Flow<List<CalendarEvent>> =
        dao.getEventsByFolder(folderGuid).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEventById(id: Long): CalendarEvent? =
        dao.getEventById(id)?.toDomain()

    override suspend fun addEvent(event: CalendarEvent): DomainResult<Long> =
        runCatching { dao.insertEvent(event.toEntity()) }
            .fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun updateEvent(event: CalendarEvent): DomainResult<Unit> =
        runCatching { dao.updateEvent(event.toEntity()) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun deleteEvent(id: Long): DomainResult<Unit> =
        runCatching { dao.deleteEventById(id) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun getUpcomingEvents(fromMillis: Long, toMillis: Long): DomainResult<List<CalendarEvent>> =
        runCatching { dao.getUpcomingEvents(fromMillis, toMillis).map { it.toDomain() } }
            .fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun getPendingReminders(fromMillis: Long): DomainResult<List<CalendarEvent>> =
        runCatching { dao.getPendingReminders(fromMillis).map { it.toDomain() } }
            .fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { DomainResult.Error(it) },
            )
}
