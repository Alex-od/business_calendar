package ua.danichapps.radiantdays.domain.repository

import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult

interface AiCompletionClient {
    suspend fun completeConversation(messages: List<AiChatMessage>): DomainResult<String>

    suspend fun complete(resolvedPrompt: String): DomainResult<String> =
        completeConversation(listOf(AiChatMessage(AiChatRole.USER, resolvedPrompt)))
}

/** Resolves the active client (stub vs OpenAI) on each call. */
fun interface AiCompletionClientProvider {
    fun getClient(): AiCompletionClient
}
