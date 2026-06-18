package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientFactory

class ValidateAiApiKeyUseCaseTest {

    private lateinit var client: AiCompletionClient
    private lateinit var clientFactory: AiCompletionClientFactory
    private lateinit var useCase: ValidateAiApiKeyUseCase

    @Before
    fun setUp() {
        client = mockk()
        clientFactory = AiCompletionClientFactory { _, _ -> client }
        useCase = ValidateAiApiKeyUseCase(clientFactory)
    }

    @Test
    fun `returns Error when api key is blank`() = runTest {
        val result = useCase("  ", "gpt-4o")

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.AI_INVALID_API_KEY, (result as DomainResult.Error).messageKey)
    }

    @Test
    fun `returns Success when client responds`() = runTest {
        coEvery { client.complete(any()) } returns DomainResult.Success("pong")

        val result = useCase("sk-test", "gpt-4o")

        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `propagates client Error unchanged`() = runTest {
        coEvery { client.complete(any()) } returns DomainResult.Error(
            RuntimeException("401"),
            MessageKey.AI_INVALID_API_KEY,
        )

        val result = useCase("sk-bad", "gpt-4o")

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.AI_INVALID_API_KEY, (result as DomainResult.Error).messageKey)
    }
}
