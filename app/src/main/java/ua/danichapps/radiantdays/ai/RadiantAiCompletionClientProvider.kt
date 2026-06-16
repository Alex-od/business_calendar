package ua.danichapps.radiantdays.ai

import okhttp3.OkHttpClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.locale.AppStrings

class RadiantAiCompletionClientProvider(
    private val keyStore: AiApiKeyStore,
    private val okHttpClient: OkHttpClient,
    private val appStrings: AppStrings,
) : AiCompletionClientProvider {

    private val stub by lazy { StubAiCompletionClient(appStrings) }

    override fun getClient(): AiCompletionClient {
        val key = keyStore.getKey()?.trim().orEmpty()
        return if (key.isBlank()) {
            stub
        } else {
            OpenAiCompletionClient(
                apiKey = key,
                model = keyStore.getModelId(),
                okHttpClient = okHttpClient,
            )
        }
    }
}
