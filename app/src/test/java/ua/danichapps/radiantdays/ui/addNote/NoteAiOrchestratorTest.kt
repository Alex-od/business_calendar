package ua.danichapps.radiantdays.ui.addNote

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.locale.AppLocaleStore
import java.util.Locale

class NoteAiOrchestratorTest {

    private lateinit var apiKeyStore: AiApiKeyStore
    private lateinit var localeStore: AppLocaleStore
    private lateinit var getVisibleAiActionsUseCase: GetVisibleAiActionsUseCase
    private lateinit var runAiActionUseCase: RunAiActionUseCase
    private lateinit var continueAiChatUseCase: ContinueAiChatUseCase

    private var state = AddEditNoteUiState()
    private val errors = mutableListOf<Triple<MessageKey, List<String>, Throwable?>>()
    private var autoSaveScheduled = false
    private var syncedDescription: String? = null

    @Before
    fun setUp() {
        apiKeyStore = mockk()
        localeStore = mockk()
        getVisibleAiActionsUseCase = mockk()
        runAiActionUseCase = mockk(relaxed = true)
        continueAiChatUseCase = mockk()

        every { getVisibleAiActionsUseCase() } returns flowOf(emptyList())
        every { localeStore.resolveLocale() } returns Locale.US
        state = AddEditNoteUiState()
        errors.clear()
        autoSaveScheduled = false
        syncedDescription = null
    }

    @Test
    fun `ai button does nothing when description is blank`() = runTest {
        every { apiKeyStore.hasKey() } returns true
        state = AddEditNoteUiState(description = "  ", isAiKeySaved = true)

        orchestrator(backgroundScope).onAiButtonClick()

        assertFalse(state.aiSheetVisible)
    }

    @Test
    fun `ai button opens sheet when key saved and description present`() = runTest {
        every { apiKeyStore.hasKey() } returns true
        state = AddEditNoteUiState(description = "Note text", isAiKeySaved = true)

        orchestrator(backgroundScope).onAiButtonClick()

        assertTrue(state.aiSheetVisible)
    }

    @Test
    fun `delete assistant removes paired action user message`() = runTest {
        val user = AiChatMessage(AiChatRole.USER, "prompt", actionLabel = "Improve")
        val assistant = AiChatMessage(AiChatRole.ASSISTANT, "response")
        state = AddEditNoteUiState(aiChatMessages = listOf(user, assistant))

        orchestrator(backgroundScope).onChatMessageDelete(index = 1)

        assertTrue(state.aiChatMessages.isEmpty())
        assertTrue(autoSaveScheduled)
    }

    @Test
    fun `edit first user message syncs description`() = runTest {
        val user = AiChatMessage(AiChatRole.USER, "old")
        state = AddEditNoteUiState(description = "old", aiChatMessages = listOf(user))

        orchestrator(backgroundScope).onChatMessageEdit(index = 0, content = "updated")

        assertEquals("updated", state.aiChatMessages.first().content)
        assertEquals("updated", syncedDescription)
        assertTrue(autoSaveScheduled)
    }

    @Test
    fun `chat send ignores blank message`() = runTest {
        orchestrator(backgroundScope).onChatSend("   ")

        assertTrue(state.aiChatMessages.isEmpty())
        assertFalse(state.aiChatLoading)
    }

    @Test
    fun `chat send ignores when already loading`() = runTest {
        state = AddEditNoteUiState(aiChatLoading = true)

        orchestrator(backgroundScope).onChatSend("hello")

        assertTrue(state.aiChatMessages.isEmpty())
    }

    private fun orchestrator(scope: CoroutineScope) = NoteAiOrchestrator(
        scope = scope,
        apiKeyStore = apiKeyStore,
        localeStore = localeStore,
        getVisibleAiActionsUseCase = getVisibleAiActionsUseCase,
        runAiActionUseCase = runAiActionUseCase,
        continueAiChatUseCase = continueAiChatUseCase,
        readState = { state },
        updateState = { transform -> state = transform(state) },
        onShowError = { key, args, cause -> errors += Triple(key, args, cause) },
        onScheduleAutoSave = { autoSaveScheduled = true },
        onSyncDescriptionFromFirstChatMessage = { syncedDescription = it },
    )
}
