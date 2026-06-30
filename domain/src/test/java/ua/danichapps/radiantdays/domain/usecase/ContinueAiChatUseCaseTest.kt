package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider

class ContinueAiChatUseCaseTest {

    private lateinit var client: AiCompletionClient
    private lateinit var clientProvider: AiCompletionClientProvider
    private lateinit var useCase: ContinueAiChatUseCase

    @Before
    fun setUp() {
        client = mockk()
        clientProvider = AiCompletionClientProvider { client }
        useCase = ContinueAiChatUseCase(clientProvider)
    }

    @Test
    fun `returns Error when message is blank`() = runTest {
        val result = useCase(emptyList(), "   ")

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.CHAT_MESSAGE_REQUIRED, (result as DomainResult.Error).messageKey)
    }

    @Test
    fun `passes history plus new user message to client`() = runTest {
        val history = listOf(
            AiChatMessage(AiChatRole.USER, "First"),
            AiChatMessage(AiChatRole.ASSISTANT, "Reply"),
        )
        val messagesSlot = slot<List<AiChatMessage>>()
        coEvery { client.completeConversation(capture(messagesSlot)) } returns DomainResult.Success("Next")

        val result = useCase(history, "Follow up")

        assertTrue(result is DomainResult.Success)
        assertEquals(3, messagesSlot.captured.size)
        assertEquals(AiChatRole.USER, messagesSlot.captured[0].role)
        assertEquals("First", messagesSlot.captured[0].content)
        assertEquals(AiChatRole.ASSISTANT, messagesSlot.captured[1].role)
        assertEquals("Reply", messagesSlot.captured[1].content)
        assertEquals(AiChatRole.USER, messagesSlot.captured[2].role)
        assertEquals("Follow up", messagesSlot.captured[2].content)
    }

    @Test
    fun `appends exactly one user message without duplicate`() = runTest {
        val messagesSlot = slot<List<AiChatMessage>>()
        coEvery { client.completeConversation(capture(messagesSlot)) } returns DomainResult.Success("Ok")

        useCase(emptyList(), "Hello")

        val userMessages = messagesSlot.captured.filter { it.role == AiChatRole.USER }
        assertEquals(1, userMessages.size)
        assertEquals("Hello", userMessages.single().content)
        coVerify(exactly = 1) { client.completeConversation(any()) }
    }
}
