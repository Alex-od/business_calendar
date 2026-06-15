package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider

class ContinueAiChatUseCase(
    private val clientProvider: AiCompletionClientProvider,
) {
    suspend operator fun invoke(
        history: List<AiChatMessage>,
        userMessage: String,
    ): DomainResult<String> {
        val trimmed = userMessage.trim()
        if (trimmed.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Message is required"),
                "Введите сообщение",
            )
        }
        val messages = history + AiChatMessage(AiChatRole.USER, trimmed)
        return clientProvider.getClient().completeConversation(messages)
    }
}
