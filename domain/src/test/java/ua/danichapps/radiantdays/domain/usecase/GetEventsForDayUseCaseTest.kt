package ua.danichapps.radiantdays.domain.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class GetEventsForDayUseCaseTest {

    private lateinit var repository: CalendarEventRepository
    private lateinit var useCase: GetEventsForDayUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase    = GetEventsForDayUseCase(repository)
    }

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val from   = 0L
        val to     = 86_400_000L
        val events = listOf(
            CalendarEvent(id = 1L, description = "Meeting", startTimeMillis = 1000L, endTimeMillis = 2000L),
        )
        every { repository.getEventsForDay(from, to) } returns flowOf(events)

        val result = useCase(from, to).toList()

        verify(exactly = 1) { repository.getEventsForDay(from, to) }
        assertEquals(listOf(events), result)
    }

    @Test
    fun `emits empty list when no events on day`() = runTest {
        every { repository.getEventsForDay(any(), any()) } returns flowOf(emptyList())

        val result = useCase(0L, 1000L).toList()

        assertEquals(listOf(emptyList<CalendarEvent>()), result)
    }
}
