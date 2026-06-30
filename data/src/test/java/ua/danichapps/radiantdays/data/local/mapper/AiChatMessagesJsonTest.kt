package ua.danichapps.radiantdays.data.local.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole

class AiChatMessagesJsonTest {

    @Test
    fun `round-trip preserves role content apiContent and actionLabel`() {
        val messages = listOf(
            AiChatMessage(
                role = AiChatRole.USER,
                content = "Note text",
                apiContent = "Improve: Note text",
                actionLabel = "Improve",
            ),
            AiChatMessage(AiChatRole.ASSISTANT, "Improved text"),
        )

        val restored = messages.toJson().toAiChatMessages()

        assertEquals(2, restored.size)
        assertEquals(AiChatRole.USER, restored[0].role)
        assertEquals("Note text", restored[0].content)
        assertEquals("Improve: Note text", restored[0].apiContent)
        assertEquals("Improve", restored[0].actionLabel)
        assertEquals(AiChatRole.ASSISTANT, restored[1].role)
        assertEquals("Improved text", restored[1].content)
    }

    @Test
    fun `empty json array returns empty list`() {
        assertTrue("[]".toAiChatMessages().isEmpty())
    }

    @Test
    fun `blank string returns empty list`() {
        assertTrue("".toAiChatMessages().isEmpty())
        assertTrue("   ".toAiChatMessages().isEmpty())
    }

    @Test
    fun `ignores unknown keys in json`() {
        val json = """
            [
              {
                "role": "user",
                "content": "Hello",
                "unknown_field": "ignored"
              }
            ]
        """.trimIndent()

        val restored = json.toAiChatMessages()

        assertEquals(1, restored.size)
        assertEquals("Hello", restored[0].content)
        assertEquals(AiChatRole.USER, restored[0].role)
    }
}
