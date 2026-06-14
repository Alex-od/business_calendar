package ua.danichapps.radiantdays.domain.repository

import ua.danichapps.radiantdays.domain.model.DomainResult

interface AiCompletionClient {
    suspend fun complete(resolvedPrompt: String): DomainResult<String>
}

/** Resolves the active client (stub vs OpenAI) on each call. */
fun interface AiCompletionClientProvider {
    fun getClient(): AiCompletionClient
}
