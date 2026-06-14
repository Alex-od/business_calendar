package ua.danichapps.radiantdays.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.danichapps.radiantdays.data.local.dao.CalendarEventDao
import ua.danichapps.radiantdays.data.local.dao.NoteTagDao
import ua.danichapps.radiantdays.data.local.mapper.toDomain
import ua.danichapps.radiantdays.data.local.mapper.toEntity
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class CalendarEventRepositoryImpl(
    private val dao: CalendarEventDao,
    private val noteTagDao: NoteTagDao,
) : CalendarEventRepository {

    override fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEvent>> =
        dao.getEventsForDay(dayStartMillis, dayEndMillis)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAllEvents(): Flow<List<CalendarEvent>> =
        dao.getAllEvents().map { rows -> rows.map { it.toDomain() } }

    override fun getEventsWithoutTags(): Flow<List<CalendarEvent>> =
        dao.getEventsWithoutTags().map { rows -> rows.map { it.toDomain() } }

    override fun getEventsByTagGuid(tagGuid: String): Flow<List<CalendarEvent>> =
        dao.getEventsByTag(tagGuid).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEventById(id: Long): CalendarEvent? =
        dao.getEventById(id)?.toDomain()

    override suspend fun addEvent(event: CalendarEvent): DomainResult<Long> =
        runCatching {
            val now = System.currentTimeMillis()
            val entity = event.toEntity().copy(createdAtMillis = now, updatedAtMillis = now)
            val id = dao.insertEvent(entity)
            noteTagDao.replaceTagsForNote(id, event.tagGuids)
            id
        }.fold(
            onSuccess = { DomainResult.Success(it) },
            onFailure = { DomainResult.Error(it) },
        )

    override suspend fun updateEvent(event: CalendarEvent): DomainResult<Unit> =
        runCatching {
            val now = System.currentTimeMillis()
            val existingCreatedAt = dao.getEventById(event.id)?.note?.createdAtMillis ?: now
            val entity = event.toEntity().copy(
                createdAtMillis = existingCreatedAt,
                updatedAtMillis = now,
            )
            dao.updateEvent(entity)
            noteTagDao.replaceTagsForNote(event.id, event.tagGuids)
        }.fold(
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
