package ua.danichapps.radiantdays.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import java.io.IOException
import java.net.SocketTimeoutException

class OpenAiCompletionClient(
    private val apiKey: String,
    private val model: String,
    private val okHttpClient: OkHttpClient,
) : AiCompletionClient {

    override suspend fun completeConversation(messages: List<AiChatMessage>): DomainResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val messagesArray = org.json.JSONArray()
                for (message in messages) {
                    messagesArray.put(
                        JSONObject()
                            .put("role", message.role.toApiRole())
                            .put("content", message.content),
                    )
                }

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messagesArray)
                    .toString()

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.code == 401) {
                        throw OpenAiException(MessageKey.AI_INVALID_API_KEY)
                    }
                    if (!response.isSuccessful) {
                        throw OpenAiException(
                            MessageKey.AI_HTTP_ERROR,
                            listOf(response.code.toString(), responseBody),
                        )
                    }
                    val parsed = JSONObject(responseBody)
                    val content = parsed
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    if (content.isBlank()) {
                        throw OpenAiException(MessageKey.AI_EMPTY_RESPONSE)
                    }
                    content
                }
            }.fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { throwable -> mapFailure(throwable) },
            )
        }

    private fun AiChatRole.toApiRole(): String = when (this) {
        AiChatRole.USER -> "user"
        AiChatRole.ASSISTANT -> "assistant"
    }

    private fun mapFailure(throwable: Throwable): DomainResult.Error {
        if (throwable is OpenAiException) {
            return DomainResult.Error(throwable, throwable.messageKey, throwable.messageArgs)
        }
        if (throwable is SocketTimeoutException || throwable.cause is SocketTimeoutException) {
            return DomainResult.Error(
                throwable,
                MessageKey.AI_TIMEOUT,
                listOf(AI_HTTP_READ_TIMEOUT_SECONDS.toString()),
            )
        }
        if (throwable is IOException && throwable.message.orEmpty().contains("timeout", ignoreCase = true)) {
            return DomainResult.Error(
                throwable,
                MessageKey.AI_TIMEOUT,
                listOf(AI_HTTP_READ_TIMEOUT_SECONDS.toString()),
            )
        }
        return DomainResult.Error(throwable, MessageKey.AI_REQUEST_FAILED)
    }

    private class OpenAiException(
        val messageKey: MessageKey,
        val messageArgs: List<String> = emptyList(),
    ) : Exception(messageKey.name)

    private companion object {
        const val API_URL = "https://api.openai.com/v1/chat/completions"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
