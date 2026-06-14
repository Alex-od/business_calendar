package ua.danichapps.radiantdays.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.data.local.dao.CalendarEventDao
import ua.danichapps.radiantdays.data.local.dao.NoteTagDao
import ua.danichapps.radiantdays.data.local.entity.NoteEntity
import ua.danichapps.radiantdays.data.local.entity.NoteWithTags
import ua.danichapps.radiantdays.domain.model.DomainResult

class CalendarEventRepositoryImplTest {

    private lateinit var dao: CalendarEventDao
    private lateinit var noteTagDao: NoteTagDao
    private lateinit var repository: CalendarEventRepositoryImpl

    @Before
    fun setUp() {
        dao         = mockk(relaxed = true)
        noteTagDao  = mockk(relaxed = true)
        repository  = CalendarEventRepositoryImpl(dao, noteTagDao)
    }

    @Test
    fun `getEventsForDay maps entities to domain models`() = runTest {
        val entity = sampleNoteWithTags()
        every { dao.getEventsForDay(any(), any()) } returns flowOf(listOf(entity))

        val result = repository.getEventsForDay(0L, 86_400_000L).toList()

        assertEquals(1, result.first().size)
        assertEquals(entity.note.description, result.first().first().description)
    }

    @Test
    fun `addEvent returns Success with generated ID`() = runTest {
        coEvery { dao.insertEvent(any()) } returns 99L

        val result = repository.addEvent(
            ua.danichapps.radiantdays.domain.model.CalendarEvent(
                description     = "Test",
                startTimeMillis = 1_000L,
                endTimeMillis   = 2_000L,
            )
        )

        assertTrue(result is DomainResult.Success)
        assertEquals(99L, (result as DomainResult.Success).data)
    }

    @Test
    fun `addEvent returns Error when DAO throws`() = runTest {
        coEvery { dao.insertEvent(any()) } throws RuntimeException("DB full")

        val result = repository.addEvent(
            ua.danichapps.radiantdays.domain.model.CalendarEvent(
                description     = "Test",
                startTimeMillis = 1_000L,
                endTimeMillis   = 2_000L,
            )
        )

        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `deleteEvent delegates to DAO and returns Success`() = runTest {
        coEvery { dao.deleteEventById(1L) } returns 1

        val result = repository.deleteEvent(1L)

        coVerify(exactly = 1) { dao.deleteEventById(1L) }
        assertTrue(result is DomainResult.Success)
    }

    // в”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private fun sampleNoteWithTags() = NoteWithTags(
        note = sampleEntity(),
        tags = emptyList(),
    )

    private fun sampleEntity() = NoteEntity(
        id                       = 1L,
        description              = "Demo event",
        startTimeMillis          = 1_000L,
        endTimeMillis            = 2_000L,
        isAllDay                 = false,
        color                    = "DEFAULT",
        notificationMinutesBefore = 30,
        createdAtMillis          = 1_000L,
        updatedAtMillis          = 1_000L,
    )
}
