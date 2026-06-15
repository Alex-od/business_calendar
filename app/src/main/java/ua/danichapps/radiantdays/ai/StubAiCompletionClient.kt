package ua.danichapps.radiantdays.ai

import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient

class StubAiCompletionClient : AiCompletionClient {
    override suspend fun completeConversation(messages: List<AiChatMessage>): DomainResult<String> {
        val lastUserMessage = messages.lastOrNull { it.role == AiChatRole.USER }?.content
            ?: return DomainResult.Error(
                IllegalArgumentException("No user message"),
                "Нет сообщения пользователя",
            )
        val preview = lastUserMessage.take(100).replace('\n', ' ')
        return DomainResult.Success("[Заглушка AI] $preview")
    }
}
