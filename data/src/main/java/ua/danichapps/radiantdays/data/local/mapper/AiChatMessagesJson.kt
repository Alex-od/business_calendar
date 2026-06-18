package ua.danichapps.radiantdays.data.local.mapper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

@Serializable
private data class AiChatMessageDto(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
    @SerialName("api_content") val apiContent: String? = null,
    @SerialName("action_label") val actionLabel: String? = null,
)

fun List<AiChatMessage>.toJson(): String =
    json.encodeToString(map { it.toDto() })

fun String.toAiChatMessages(): List<AiChatMessage> {
    if (isBlank() || this == "[]") return emptyList()
    return runCatching {
        json.decodeFromString<List<AiChatMessageDto>>(this).map { it.toDomain() }
    }.getOrDefault(emptyList())
}

private fun AiChatMessage.toDto() = AiChatMessageDto(
    role = when (role) {
        AiChatRole.USER -> "user"
        AiChatRole.ASSISTANT -> "assistant"
    },
    content = content,
    apiContent = apiContent,
    actionLabel = actionLabel,
)

private fun AiChatMessageDto.toDomain() = AiChatMessage(
    role = when (role) {
        "assistant" -> AiChatRole.ASSISTANT
        else -> AiChatRole.USER
    },
    content = content,
    apiContent = apiContent,
    actionLabel = actionLabel,
)
