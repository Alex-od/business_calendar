package ua.danichapps.radiantdays.ai

import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.locale.AppStrings

class RadiantAiCompletionClientProvider(
    private val keyStore: AiApiKeyStore,
    private val clientFactory: OpenAiCompletionClientFactory,
    private val appStrings: AppStrings,
) : AiCompletionClientProvider {

    private val stub by lazy { StubAiCompletionClient(appStrings) }

    override fun getClient(): AiCompletionClient {
        val key = keyStore.getKey()?.trim().orEmpty()
        return if (key.isBlank()) {
            stub
        } else {
            clientFactory.createWithLogging(key, keyStore.getModelId())
        }
    }
}
