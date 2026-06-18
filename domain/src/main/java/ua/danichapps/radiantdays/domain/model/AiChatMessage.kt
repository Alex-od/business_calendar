package ua.danichapps.radiantdays.domain.model

data class AiChatMessage(
    val role: AiChatRole,
    val content: String,
    val apiContent: String? = null,
    val actionLabel: String? = null,
)

fun AiChatMessage.textForApi(): String = apiContent ?: content

fun AiChatMessage.visibleContent(noteDescription: String): String {
    if (role != AiChatRole.USER) return content
    if (noteDescription.isBlank()) return content
    if (apiContent != null && content == apiContent) return noteDescription
    if (apiContent == null && content != noteDescription && content.contains(noteDescription)) {
        return noteDescription
    }
    return content
}

fun List<AiChatMessage>.normalizeFirstUserMessage(noteDescription: String): List<AiChatMessage> {
    if (isEmpty() || noteDescription.isBlank()) return this
    val first = first()
    if (first.role != AiChatRole.USER) return this
    return when {
        first.apiContent != null && first.content == first.apiContent ->
            listOf(first.copy(content = noteDescription)) + drop(1)
        first.apiContent == null && first.content != noteDescription && first.content.contains(noteDescription) ->
            listOf(first.copy(content = noteDescription, apiContent = first.content)) + drop(1)
        else -> this
    }
}
