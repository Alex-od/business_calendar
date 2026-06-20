package ua.danichapps.radiantdays.domain.model

data class AiActionResult(
    val userMessage: AiChatMessage,
    val response: String,
)
