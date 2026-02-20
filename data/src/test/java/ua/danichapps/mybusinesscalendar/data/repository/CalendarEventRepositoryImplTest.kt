package ua.danichapps.mybusinesscalendar.data.repository

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
import ua.danichapps.mybusinesscalendar.data.local.dao.CalendarEventDao
import ua.danichapps.mybusinesscalendar.data.local.entity.CalendarEventEntity
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult

class CalendarEventRepositoryImplTest {

    private lateinit var dao: CalendarEventDao
    private lateinit var repository: CalendarEventRepositoryImpl

    @Before
    fun setUp() {
        dao        = mockk()
        repository = CalendarEventRepositoryImpl(dao)
    }

    @Test
    fun `getEventsForDay maps entities to domain models`() = runTest {
        val entity = sampleEntity()
        every { dao.getEventsForDay(any(), any()) } returns flowOf(listOf(entity))

        val result = repository.getEventsForDay(0L, 86_400_000L).toList()

        assertEquals(1, result.first().size)
        assertEquals(entity.title, result.first().first().title)
    }

    @Test
    fun `addEvent returns Success with generated ID`() = runTest {
        coEvery { dao.insertEvent(any()) } returns 99L

        val result = repository.addEvent(
            ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent(
                title           = "Test",
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
            ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent(
                title           = "Test",
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sampleEntity() = CalendarEventEntity(
        id                       = 1L,
        title                    = "Demo event",
        description              = "",
        startTimeMillis          = 1_000L,
        endTimeMillis            = 2_000L,
        isAllDay                 = false,
        color                    = "DEFAULT",
        notificationMinutesBefore = 30,
    )
}
