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
