package ua.danichapps.radiantdays.ai

import okhttp3.OkHttpClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientFactory

class OpenAiCompletionClientFactory(
    private val okHttpClient: OkHttpClient,
    private val logSink: AiApiRequestLogSink,
) : AiCompletionClientFactory {

    override fun create(apiKey: String, modelId: String): AiCompletionClient =
        buildClient(
            apiKey = apiKey,
            modelId = modelId,
            shouldLog = false,
        )

    fun createWithLogging(apiKey: String, modelId: String): AiCompletionClient =
        buildClient(
            apiKey = apiKey,
            modelId = modelId,
            shouldLog = true,
        )

    private fun buildClient(
        apiKey: String,
        modelId: String,
        shouldLog: Boolean,
    ): AiCompletionClient =
        OpenAiCompletionClient(
            apiKey = AiApiKeySanitizer.sanitize(apiKey),
            model = modelId,
            okHttpClient = okHttpClient,
            logSink = logSink,
            shouldLog = shouldLog,
            requestSource = AiApiRequestSource.AI_CHAT,
        )
}
