package ua.danichapps.radiantdays.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AiChatMessageTest {

    @Test
    fun `textForApi returns apiContent when set`() {
        val message = AiChatMessage(
            role = AiChatRole.USER,
            content = "Note text",
            apiContent = "Full resolved prompt",
        )

        assertEquals("Full resolved prompt", message.textForApi())
    }

    @Test
    fun `textForApi returns content when apiContent is null`() {
        val message = AiChatMessage(AiChatRole.USER, "Hello")

        assertEquals("Hello", message.textForApi())
    }

    @Test
    fun `visibleContent shows note description for action message with apiContent`() {
        val message = AiChatMessage(
            role = AiChatRole.USER,
            content = "Improve this: Note text",
            apiContent = "Improve this: Note text",
            actionLabel = "Improve",
        )

        assertEquals("Note text", message.visibleContent("Note text"))
    }

    @Test
    fun `visibleContent returns assistant content unchanged`() {
        val message = AiChatMessage(AiChatRole.ASSISTANT, "Response")

        assertEquals("Response", message.visibleContent("Note text"))
    }

    @Test
    fun `normalizeFirstUserMessage replaces content when apiContent matches`() {
        val messages = listOf(
            AiChatMessage(
                role = AiChatRole.USER,
                content = "Improve this: old",
                apiContent = "Improve this: old",
                actionLabel = "Improve",
            ),
            AiChatMessage(AiChatRole.ASSISTANT, "Done"),
        )

        val normalized = messages.normalizeFirstUserMessage("updated note")

        assertEquals("updated note", normalized[0].content)
        assertEquals("Improve this: old", normalized[0].apiContent)
        assertEquals("Done", normalized[1].content)
    }

    @Test
    fun `normalizeFirstUserMessage returns list unchanged when first is assistant`() {
        val messages = listOf(AiChatMessage(AiChatRole.ASSISTANT, "Hi"))

        assertEquals(messages, messages.normalizeFirstUserMessage("note"))
    }
}
