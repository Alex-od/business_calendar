package ua.danichapps.radiantdays.ai

import okhttp3.OkHttpClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientFactory

class OpenAiCompletionClientFactory(
    private val okHttpClient: OkHttpClient,
) : AiCompletionClientFactory {

    override fun create(apiKey: String, modelId: String): AiCompletionClient =
        OpenAiCompletionClient(
            apiKey = AiApiKeySanitizer.sanitize(apiKey),
            model = modelId,
            okHttpClient = okHttpClient,
        )
}
