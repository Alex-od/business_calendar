package ua.danichapps.radiantdays.ai

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient

class StubAiCompletionClient : AiCompletionClient {
    override suspend fun complete(resolvedPrompt: String): DomainResult<String> {
        val preview = resolvedPrompt.take(100).replace('\n', ' ')
        return DomainResult.Success("[Заглушка AI] $preview")
    }
}
