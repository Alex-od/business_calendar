package ua.danichapps.radiantdays.domain.util

import ua.danichapps.radiantdays.domain.model.AiNoteContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiPromptTemplate {

    fun resolve(prompt: String, context: AiNoteContext): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", context.locale)
        return prompt
            .replace("{{text}}", context.text)
            .replace("{{title}}", context.title)
            .replace("{{tags}}", context.tagNames.joinToString(", "))
            .replace("{{date}}", dateFormat.format(Date(context.noteDateMillis)))
    }
}
