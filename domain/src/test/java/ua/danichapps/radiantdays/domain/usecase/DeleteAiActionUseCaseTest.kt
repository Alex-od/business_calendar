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
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class DeleteAiActionUseCaseTest {

    private lateinit var repository: AiActionRepository
    private lateinit var useCase: DeleteAiActionUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = DeleteAiActionUseCase(repository)
    }

    @Test
    fun `returns Error when action is built-in`() = runTest {
        val builtIn = AiAction(
            guid = AiAction.BUILTIN_IMPROVE_GUID,
            name = "Улучшить",
            prompt = "test",
            isBuiltIn = true,
        )
        coEvery { repository.getActionByGuid(AiAction.BUILTIN_IMPROVE_GUID) } returns builtIn

        val result = useCase(AiAction.BUILTIN_IMPROVE_GUID)

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.AI_ACTION_BUILTIN_DELETE, (result as DomainResult.Error).messageKey)
        coVerify(exactly = 0) { repository.deleteAction(any()) }
    }

    @Test
    fun `delegates to repository for custom action`() = runTest {
        coEvery { repository.getActionByGuid("custom") } returns AiAction(
            guid = "custom",
            name = "Custom",
            prompt = "test",
        )
        coEvery { repository.deleteAction("custom") } returns DomainResult.Success(Unit)

        val result = useCase("custom")

        assertTrue(result is DomainResult.Success)
        coVerify(exactly = 1) { repository.deleteAction("custom") }
    }
}
