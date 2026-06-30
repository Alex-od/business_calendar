package ua.danichapps.radiantdays.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiApiRequestLogFormatterTest {

    @Test
    fun `formatHttp includes method url headers bodies and source`() {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer test-key")
            .addHeader("Content-Type", "application/json")
            .post("""{"model":"gpt-4o"}""".toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("content-type", "application/json")
            .body("""{"choices":[{"message":{"content":"Hi"}}]}""".toResponseBody(JSON_MEDIA_TYPE))
            .build()

        val log = AiApiRequestLogFormatter.formatHttp(
            source = AiApiRequestSource.AI_CHAT,
            request = request,
            requestBody = """{"model":"gpt-4o"}""",
            response = response,
            responseBody = """{"choices":[{"message":{"content":"Hi"}}]}""",
        )

        assertTrue(log.contains("Source: AI_CHAT"))
        assertTrue(log.contains("POST https://api.openai.com/v1/chat/completions"))
        assertTrue(log.contains("Authorization: Bearer test-key"))
        assertTrue(log.contains("=== REQUEST ==="))
        assertTrue(log.contains("=== RESPONSE ==="))
        assertTrue(log.contains("HTTP 200"))
        assertTrue(log.contains("\"model\""))
        assertTrue(log.contains("gpt-4o"))
        assertTrue(log.contains("\"content\""))
        assertTrue(log.contains("Hi"))
    }

    @Test
    fun `formatHttp includes messages array in pretty-printed body`() {
        val requestBody = """
            {
              "model": "gpt-4o",
              "messages": [
                {"role": "user", "content": "Question"},
                {"role": "assistant", "content": "Answer"}
              ]
            }
        """.trimIndent()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"choices":[{"message":{"content":"Hi"}}]}""".toResponseBody(JSON_MEDIA_TYPE))
            .build()

        val log = AiApiRequestLogFormatter.formatHttp(
            source = AiApiRequestSource.AI_CHAT,
            request = request,
            requestBody = requestBody,
            response = response,
            responseBody = """{"choices":[{"message":{"content":"Hi"}}]}""",
        )

        assertTrue(log.contains("\"messages\""))
        assertTrue(log.contains("Question"))
        assertTrue(log.contains("Answer"))
        assertTrue(log.contains("\"role\""))
    }

    @Test
    fun `formatNetworkError includes request and exception`() {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post("""{"model":"gpt-4o"}""".toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val log = AiApiRequestLogFormatter.formatNetworkError(
            source = AiApiRequestSource.AI_CHAT,
            request = request,
            requestBody = """{"model":"gpt-4o"}""",
            error = java.net.SocketTimeoutException("timeout"),
        )

        assertTrue(log.contains("=== ERROR ==="))
        assertTrue(log.contains("SocketTimeoutException: timeout"))
        assertTrue(log.contains("=== REQUEST ==="))
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
