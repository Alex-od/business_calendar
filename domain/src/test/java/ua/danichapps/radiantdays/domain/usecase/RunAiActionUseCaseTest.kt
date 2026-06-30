package ua.danichapps.radiantdays.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.domain.localization.PassthroughAiActionLocalizer
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import java.util.Locale

class RunAiActionUseCaseTest {

    private lateinit var repository: AiActionRepository
    private lateinit var client: AiCompletionClient
    private lateinit var useCase: RunAiActionUseCase

    private val action = AiAction(
        guid = "action-1",
        name = "Improve",
        prompt = "Improve: {{text}}",
    )
    private val context = AiNoteContext(
        text = "Note body",
        title = "Title",
        noteDateMillis = 1_704_067_200_000L,
        locale = Locale.US,
    )

    @Before
    fun setUp() {
        repository = mockk()
        client = mockk()
        useCase = RunAiActionUseCase(
            repository = repository,
            clientProvider = AiCompletionClientProvider { client },
            localizer = PassthroughAiActionLocalizer,
        )
    }

    @Test
    fun `returns Error when note text is blank`() = runTest {
        val result = useCase("action-1", context.copy(text = "  "))

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.NOTE_TEXT_REQUIRED, (result as DomainResult.Error).messageKey)
    }

    @Test
    fun `returns Error when action not found`() = runTest {
        coEvery { repository.getActionByGuid("missing") } returns null

        val result = useCase("missing", context)

        assertTrue(result is DomainResult.Error)
        assertEquals(MessageKey.AI_ACTION_NOT_FOUND, (result as DomainResult.Error).messageKey)
    }

    @Test
    fun `passes history plus user message with apiContent to client`() = runTest {
        val history = listOf(
            AiChatMessage(AiChatRole.USER, "Previous"),
            AiChatMessage(AiChatRole.ASSISTANT, "Earlier reply"),
        )
        val messagesSlot = slot<List<AiChatMessage>>()
        coEvery { repository.getActionByGuid("action-1") } returns action
        coEvery { client.completeConversation(capture(messagesSlot)) } returns DomainResult.Success("Improved")

        val result = useCase("action-1", context, history)

        assertTrue(result is DomainResult.Success)
        assertEquals(3, messagesSlot.captured.size)
        assertEquals("Previous", messagesSlot.captured[0].content)
        assertEquals("Earlier reply", messagesSlot.captured[1].content)
        val newUser = messagesSlot.captured[2]
        assertEquals(AiChatRole.USER, newUser.role)
        assertEquals("Note body", newUser.content)
        assertEquals("Improve: Note body", newUser.apiContent)
        assertEquals("Improve", newUser.actionLabel)
    }

    @Test
    fun `returns AiActionResult with user message and response`() = runTest {
        coEvery { repository.getActionByGuid("action-1") } returns action
        coEvery { client.completeConversation(any()) } returns DomainResult.Success("Improved text")

        val result = useCase("action-1", context)

        assertTrue(result is DomainResult.Success)
        val actionResult = (result as DomainResult.Success).data
        assertEquals("Note body", actionResult.userMessage.content)
        assertEquals("Improve: Note body", actionResult.userMessage.apiContent)
        assertEquals("Improved text", actionResult.response)
    }
}
