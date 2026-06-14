package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class AddAiActionUseCaseTest {

    private lateinit var repository: AiActionRepository
    private lateinit var useCase: AddAiActionUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = AddAiActionUseCase(repository)
    }

    @Test
    fun `returns Error when name is blank`() = runTest {
        val result = useCase(name = "  ", description = null, prompt = "test")

        assertTrue(result is DomainResult.Error)
        assertEquals("Введите название действия", (result as DomainResult.Error).message)
        coVerify(exactly = 0) { repository.addAction(any(), any(), any(), any()) }
    }

    @Test
    fun `returns Error when prompt is blank`() = runTest {
        val result = useCase(name = "Test", description = null, prompt = "  ")

        assertTrue(result is DomainResult.Error)
        assertEquals("Введите промпт", (result as DomainResult.Error).message)
        coVerify(exactly = 0) { repository.addAction(any(), any(), any(), any()) }
    }

    @Test
    fun `returns Error when name already exists`() = runTest {
        coEvery { repository.isActionNameTaken("Test") } returns true

        val result = useCase(name = "Test", description = null, prompt = "Do something")

        assertTrue(result is DomainResult.Error)
        assertEquals("Действие с таким названием уже существует", (result as DomainResult.Error).message)
    }

    @Test
    fun `delegates to repository when valid`() = runTest {
        val action = AiAction(guid = "g1", name = "Test", prompt = "Do something")
        coEvery { repository.isActionNameTaken("Test") } returns false
        coEvery { repository.addAction("Test", null, "Do something", true) } returns DomainResult.Success(action)

        val result = useCase(name = "Test", description = null, prompt = "Do something")

        assertTrue(result is DomainResult.Success)
        coVerify(exactly = 1) { repository.addAction("Test", null, "Do something", true) }
    }
}
