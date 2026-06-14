package ua.danichapps.radiantdays.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiCompletionClient

class OpenAiCompletionClient(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient,
) : AiCompletionClient {

    override suspend fun complete(resolvedPrompt: String): DomainResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("model", MODEL)
                    .put(
                        "messages",
                        org.json.JSONArray().put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", resolvedPrompt),
                        ),
                    )
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
                        throw OpenAiException("Неверный API-ключ. Проверьте Settings")
                    }
                    if (!response.isSuccessful) {
                        throw OpenAiException("OpenAI error ${response.code}: $responseBody")
                    }
                    val parsed = JSONObject(responseBody)
                    val content = parsed
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    if (content.isBlank()) {
                        throw OpenAiException("Пустой ответ от OpenAI")
                    }
                    content
                }
            }.fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = { throwable ->
                    DomainResult.Error(
                        throwable,
                        throwable.message ?: "Не удалось выполнить AI-запрос",
                    )
                },
            )
        }

    private class OpenAiException(message: String) : Exception(message)

    private companion object {
        const val API_URL = "https://api.openai.com/v1/chat/completions"
        const val MODEL = "gpt-4o-mini"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
