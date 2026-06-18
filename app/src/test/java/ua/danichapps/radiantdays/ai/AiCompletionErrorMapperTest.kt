package ua.danichapps.radiantdays.ai

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class AiCompletionErrorMapperTest {

    @Test
    fun `maps invalid api key from OpenAiException`() {
        val error = AiCompletionErrorMapper.mapFailure(
            AiCompletionErrorMapper.OpenAiException(MessageKey.AI_INVALID_API_KEY),
        )

        assertEquals(MessageKey.AI_INVALID_API_KEY, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `maps SSL handshake exception`() {
        val error = AiCompletionErrorMapper.mapFailure(SSLHandshakeException("handshake failed"))

        assertEquals(MessageKey.AI_TLS_ERROR, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `maps IOException with handshake message`() {
        val error = AiCompletionErrorMapper.mapFailure(IOException("SSL handshake aborted"))

        assertEquals(MessageKey.AI_TLS_ERROR, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `maps unknown host in cause chain`() {
        val error = AiCompletionErrorMapper.mapFailure(
            IOException("failed", UnknownHostException("api.openai.com")),
        )

        assertEquals(MessageKey.AI_NO_NETWORK, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `maps socket exception to no network`() {
        val error = AiCompletionErrorMapper.mapFailure(SocketException("Connection reset"))

        assertEquals(MessageKey.AI_NO_NETWORK, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `maps invalid authorization header characters to invalid api key`() {
        val error = AiCompletionErrorMapper.mapFailure(
            IllegalArgumentException("Unexpected char 0x0d at 91 in Authorization value"),
        )

        assertEquals(MessageKey.AI_INVALID_API_KEY, (error as DomainResult.Error).messageKey)
    }

    @Test
    fun `falls back to request failed with root message`() {
        val error = AiCompletionErrorMapper.mapFailure(IOException("unexpected vendor error"))

        assertEquals(MessageKey.AI_REQUEST_FAILED, (error as DomainResult.Error).messageKey)
        assertEquals("unexpected vendor error", error.messageArgs.first())
    }
}
