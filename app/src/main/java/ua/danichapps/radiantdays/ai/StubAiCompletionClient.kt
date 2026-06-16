package ua.danichapps.radiantdays.ai

import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.locale.AppStrings

class StubAiCompletionClient(
    private val appStrings: AppStrings,
) : AiCompletionClient {
    override suspend fun completeConversation(messages: List<AiChatMessage>): DomainResult<String> {
        val lastUserMessage = messages.lastOrNull { it.role == AiChatRole.USER }?.content
            ?: return DomainResult.Error(
                IllegalArgumentException("No user message"),
                MessageKey.CHAT_MESSAGE_REQUIRED,
            )
        val preview = lastUserMessage.take(100).replace('\n', ' ')
        return DomainResult.Success(appStrings.aiStubResponse(preview))
    }
}
