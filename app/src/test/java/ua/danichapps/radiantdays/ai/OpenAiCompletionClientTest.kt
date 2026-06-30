package ua.danichapps.radiantdays.ai

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OpenAiCompletionClientTest {

    private lateinit var server: MockWebServer
    private lateinit var logSink: RecordingLogSink
    private lateinit var client: OpenAiCompletionClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        logSink = RecordingLogSink()
        client = createClient(shouldLog = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `completeConversation returns Success with trimmed assistant content`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("  Hello AI  ")),
        )

        val result = client.completeConversation(
            listOf(AiChatMessage(AiChatRole.USER, "Hi")),
        )

        assertTrue(result is DomainResult.Success)
        assertEquals("Hello AI", (result as DomainResult.Success).data)

        val request = server.takeRequest()
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"model\":\"gpt-4o\""))

        val log = logSink.savedLog.orEmpty()
        assertTrue(log.contains("=== REQUEST ==="))
        assertTrue(log.contains("=== RESPONSE ==="))
    }

    @Test
    fun `does not save log when shouldLog is false`() = runTest {
        client = createClient(shouldLog = false)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("Hi")),
        )

        client.complete("Hi")

        assertNull(logSink.savedLog)
    }

    @Test
    fun `returns Error with AI_INVALID_API_KEY when response is 401`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Invalid API key"}}"""),
        )

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_INVALID_API_KEY)
        val log = logSink.savedLog.orEmpty()
        assertTrue(log.contains("HTTP 401"))
        assertTrue(log.contains("Invalid API key"))
    }

    @Test
    fun `saves network error log when connection fails`() = runTest {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val result = client.complete("Hi")

        assertTrue(result is DomainResult.Error)
        val log = logSink.savedLog.orEmpty()
        assertTrue(log.contains("=== ERROR ==="))
        assertTrue(log.contains("=== REQUEST ==="))
    }

    @Test
    fun `returns Error with AI_MODEL_NOT_FOUND when response is 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_MODEL_NOT_FOUND)
    }

    @Test
    fun `returns Error with AI_RATE_LIMIT when response is 429`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_RATE_LIMIT)
    }

    @Test
    fun `returns Error with AI_SERVER_ERROR when response is 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_SERVER_ERROR)
    }

    @Test
    fun `returns Error with AI_HTTP_ERROR when response is 400`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"Bad request details"}}"""),
        )

        val result = client.complete("Hi")

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertEquals(MessageKey.AI_HTTP_ERROR, error.messageKey)
        assertEquals(listOf("400", "Bad request details"), error.messageArgs)
    }

    @Test
    fun `returns Error with AI_EMPTY_RESPONSE when assistant content is blank`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("   ")),
        )

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_EMPTY_RESPONSE)
    }

    @Test
    fun `returns Error with AI_PARSE_ERROR when body is not valid JSON`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("not-json"),
        )

        val result = client.complete("Hi")

        assertError(result, MessageKey.AI_PARSE_ERROR)
    }

    @Test
    fun `completeConversation sends multi-message history in order`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("Done")),
        )

        client.completeConversation(
            listOf(
                AiChatMessage(AiChatRole.USER, "First"),
                AiChatMessage(AiChatRole.ASSISTANT, "Reply"),
                AiChatMessage(AiChatRole.USER, "Follow up"),
            ),
        )

        val requestBody = server.takeRequest().body.readUtf8()
        val firstIndex = requestBody.indexOf("\"First\"")
        val replyIndex = requestBody.indexOf("\"Reply\"")
        val followUpIndex = requestBody.indexOf("\"Follow up\"")
        assertTrue(firstIndex in 0 until replyIndex)
        assertTrue(replyIndex in 0 until followUpIndex)
        assertTrue(requestBody.contains("\"role\":\"user\""))
        assertTrue(requestBody.contains("\"role\":\"assistant\""))
    }

    @Test
    fun `completeConversation uses apiContent in request body`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("Ok")),
        )

        client.completeConversation(
            listOf(
                AiChatMessage(
                    role = AiChatRole.USER,
                    content = "Note text",
                    apiContent = "Resolved prompt for API",
                ),
            ),
        )

        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("Resolved prompt for API"))
        assertTrue(!requestBody.contains("Note text"))
    }

    @Test
    fun `log contains full message history in request section`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(chatCompletionJson("Response")),
        )

        client.completeConversation(
            listOf(
                AiChatMessage(AiChatRole.USER, "Question"),
                AiChatMessage(AiChatRole.ASSISTANT, "Answer"),
                AiChatMessage(AiChatRole.USER, "Next"),
            ),
        )

        val log = logSink.savedLog.orEmpty()
        val requestSection = log.substringAfter("=== REQUEST ===").substringBefore("=== RESPONSE ===")
        assertTrue(requestSection.contains("\"messages\""))
        assertTrue(requestSection.contains("\"role\": \"user\"") || requestSection.contains("\"role\":\"user\""))
        assertTrue(requestSection.contains("Question"))
        assertTrue(requestSection.contains("Answer"))
        assertTrue(requestSection.contains("Next"))
    }

    private fun createClient(shouldLog: Boolean): OpenAiCompletionClient =
        OpenAiCompletionClient(
            apiKey = "test-key",
            model = "gpt-4o",
            okHttpClient = OkHttpClient(),
            logSink = logSink,
            shouldLog = shouldLog,
            apiUrl = server.url("/v1/chat/completions").toString(),
        )

    private fun assertError(result: DomainResult<String>, expectedKey: MessageKey) {
        assertTrue("Expected Error but got $result", result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertEquals(
            "Unexpected failure: ${error.exception.javaClass.name}: ${error.exception.message}",
            expectedKey,
            error.messageKey,
        )
    }

    private fun chatCompletionJson(content: String): String {
        val escaped = content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        return """{"choices":[{"message":{"content":"$escaped"}}]}"""
    }
}
