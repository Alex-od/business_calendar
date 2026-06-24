package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class GetEventByIdUseCaseTest {

    private lateinit var repository: CalendarEventRepository
    private lateinit var useCase: GetEventByIdUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetEventByIdUseCase(repository)
    }

    @Test
    fun `returns event when found`() = runTest {
        val event = CalendarEvent(
            id = 42L,
            title = "Note",
            description = "Body",
            startTimeMillis = 1_000L,
            endTimeMillis = 2_000L,
        )
        coEvery { repository.getEventById(42L) } returns event

        val result = useCase(42L)

        assertEquals(event, result)
        coVerify(exactly = 1) { repository.getEventById(42L) }
    }

    @Test
    fun `returns null when not found`() = runTest {
        coEvery { repository.getEventById(99L) } returns null

        assertNull(useCase(99L))
    }
}
