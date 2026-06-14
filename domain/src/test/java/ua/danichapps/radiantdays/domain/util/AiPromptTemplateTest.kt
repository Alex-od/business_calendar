package ua.danichapps.radiantdays.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.AiNoteContext

class AiPromptTemplateTest {

    @Test
    fun `replaces all placeholders`() {
        val context = AiNoteContext(
            text = "Note body",
            title = "Title",
            tagNames = listOf("Work", "Ideas"),
            noteDateMillis = 1_704_067_200_000L, // 2024-01-01 UTC-ish
        )
        val prompt = "Title: {{title}}\nTags: {{tags}}\nDate: {{date}}\n{{text}}"

        val result = AiPromptTemplate.resolve(prompt, context)

        assertEquals(true, result.contains("Title: Title"))
        assertEquals(true, result.contains("Tags: Work, Ideas"))
        assertEquals(true, result.contains("Note body"))
        assertEquals(false, result.contains("{{"))
    }
}
