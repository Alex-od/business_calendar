package ua.danichapps.radiantdays.domain.repository

fun interface AiCompletionClientFactory {
    fun create(apiKey: String, modelId: String): AiCompletionClient
}
