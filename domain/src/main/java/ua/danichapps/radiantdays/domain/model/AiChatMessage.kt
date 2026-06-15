package ua.danichapps.radiantdays.domain.model

data class AiChatMessage(
    val role: AiChatRole,
    val content: String,
)
