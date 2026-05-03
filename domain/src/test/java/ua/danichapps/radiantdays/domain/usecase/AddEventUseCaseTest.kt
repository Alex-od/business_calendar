package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class AddEventUseCaseTest {

    private lateinit var repository: CalendarEventRepository
    private lateinit var useCase: AddEventUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase    = AddEventUseCase(repository)
    }

    @Test
    fun `returns Error when description is blank`() = runTest {
        val event = validEvent().copy(description = "  ")

        val result = useCase(event)

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("blank", ignoreCase = true))
        coVerify(exactly = 0) { repository.addEvent(any()) }
    }

    @Test
    fun `returns Error when end time is before start time`() = runTest {
        val event = validEvent().copy(startTimeMillis = 2000L, endTimeMillis = 1000L)

        val result = useCase(event)

        assertTrue(result is DomainResult.Error)
        coVerify(exactly = 0) { repository.addEvent(any()) }
    }

    @Test
    fun `delegates to repository when event is valid`() = runTest {
        val event = validEvent()
        coEvery { repository.addEvent(event) } returns DomainResult.Success(42L)

        val result = useCase(event)

        coVerify(exactly = 1) { repository.addEvent(event) }
        assertTrue(result is DomainResult.Success)
        assertEquals(42L, (result as DomainResult.Success).data)
    }

    @Test
    fun `propagates repository Error unchanged`() = runTest {
        val event     = validEvent()
        val exception = RuntimeException("DB error")
        coEvery { repository.addEvent(event) } returns DomainResult.Error(exception)

        val result = useCase(event)

        assertTrue(result is DomainResult.Error)
        assertEquals(exception, (result as DomainResult.Error).exception)
    }

    // в”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private fun validEvent() = CalendarEvent(
        description      = "Daily sync",
        startTimeMillis  = 1_000L,
        endTimeMillis    = 2_000L,
    )
}
