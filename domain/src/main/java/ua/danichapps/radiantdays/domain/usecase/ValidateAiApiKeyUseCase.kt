package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientFactory

class ValidateAiApiKeyUseCase(
    private val clientFactory: AiCompletionClientFactory,
) {
    suspend operator fun invoke(apiKey: String, modelId: String): DomainResult<Unit> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("API key is blank"),
                MessageKey.AI_INVALID_API_KEY,
            )
        }
        val client = clientFactory.create(trimmedKey, modelId)
        return when (val result = client.complete(VALIDATION_PROMPT)) {
            is DomainResult.Success -> DomainResult.Success(Unit)
            is DomainResult.Error -> result
        }
    }

    private companion object {
        const val VALIDATION_PROMPT = "ping"
    }
}
