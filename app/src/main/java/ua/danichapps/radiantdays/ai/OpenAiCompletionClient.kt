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
import ua.danichapps.radiantdays.domain.model.textForApi
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient
import java.io.IOException
import java.security.GeneralSecurityException
import org.json.JSONException

class OpenAiCompletionClient(
    private val apiKey: String,
    private val model: String,
    private val okHttpClient: OkHttpClient,
    private val logSink: AiApiRequestLogSink,
    private val shouldLog: Boolean,
    private val requestSource: AiApiRequestSource = AiApiRequestSource.AI_CHAT,
    private val apiUrl: String = DEFAULT_API_URL,
) : AiCompletionClient {

    override suspend fun completeConversation(messages: List<AiChatMessage>): DomainResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val messagesArray = org.json.JSONArray()
                for (message in messages) {
                    messagesArray.put(
                        JSONObject()
                            .put("role", message.role.toApiRole())
                            .put("content", message.textForApi()),
                    )
                }

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messagesArray)
                    .toString()

                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string().orEmpty()
                        persistLog(
                            AiApiRequestLogFormatter.formatHttp(
                                source = requestSource,
                                request = request,
                                requestBody = body,
                                response = response,
                                responseBody = responseBody,
                            ),
                        )
                        if (response.code == 401) {
                            throw AiCompletionErrorMapper.OpenAiException(MessageKey.AI_INVALID_API_KEY)
                        }
                        if (response.code == 404) {
                            throw AiCompletionErrorMapper.OpenAiException(MessageKey.AI_MODEL_NOT_FOUND, listOf(model))
                        }
                        if (response.code == 429) {
                            throw AiCompletionErrorMapper.OpenAiException(MessageKey.AI_RATE_LIMIT)
                        }
                        if (response.code >= 500) {
                            throw AiCompletionErrorMapper.OpenAiException(MessageKey.AI_SERVER_ERROR)
                        }
                        if (!response.isSuccessful) {
                            throw AiCompletionErrorMapper.OpenAiException(
                                MessageKey.AI_HTTP_ERROR,
                                listOf(response.code.toString(), extractErrorDetail(responseBody)),
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
                            throw AiCompletionErrorMapper.OpenAiException(MessageKey.AI_EMPTY_RESPONSE)
                        }
                        content
                    }
                } catch (e: Throwable) {
                    if (isLoggableFailure(e)) {
                        persistLog(
                            AiApiRequestLogFormatter.formatNetworkError(
                                source = requestSource,
                                request = request,
                                requestBody = body,
                                error = e,
                            ),
                        )
                    }
                    throw e
                }
            }.fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { throwable -> AiCompletionErrorMapper.mapFailure(throwable) },
            )
        }

    private fun persistLog(text: String) {
        if (!shouldLog) return
        logSink.save(text)
    }

    private fun isLoggableFailure(error: Throwable): Boolean {
        if (error is AiCompletionErrorMapper.OpenAiException) return false
        if (error is IOException || error is GeneralSecurityException) return true
        val cause = error.cause ?: return false
        return cause is IOException || cause is GeneralSecurityException
    }

    private fun extractErrorDetail(responseBody: String): String {
        val parsedMessage = runCatching {
            JSONObject(responseBody).optJSONObject("error")?.optString("message")?.trim()
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        return parsedMessage?.take(DETAIL_MAX_LENGTH).orEmpty()
    }

    private fun AiChatRole.toApiRole(): String = when (this) {
        AiChatRole.USER -> "user"
        AiChatRole.ASSISTANT -> "assistant"
    }

    private companion object {
        const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"
        const val DETAIL_MAX_LENGTH = 200
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
