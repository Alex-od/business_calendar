package ua.danichapps.radiantdays.ai

import org.json.JSONException
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

internal object AiCompletionErrorMapper {

    fun mapFailure(throwable: Throwable): DomainResult.Error {
        for (candidate in throwable.causeChain()) {
            classify(candidate)?.let { classified ->
                return DomainResult.Error(
                    candidate,
                    classified.messageKey,
                    classified.messageArgs,
                )
            }
        }
        val root = throwable.causeChain().last()
        return DomainResult.Error(
            throwable,
            MessageKey.AI_REQUEST_FAILED,
            listOf(root.message.orEmpty()),
        )
    }

    private fun classify(throwable: Throwable): ClassifiedError? {
        if (throwable is OpenAiException) {
            return ClassifiedError(throwable.messageKey, throwable.messageArgs)
        }
        if (throwable is SocketTimeoutException) {
            return ClassifiedError(MessageKey.AI_TIMEOUT, listOf(AI_HTTP_READ_TIMEOUT_SECONDS.toString()))
        }
        if (throwable is UnknownHostException || throwable is ConnectException) {
            return ClassifiedError(MessageKey.AI_NO_NETWORK)
        }
        if (throwable is java.net.SocketException) {
            return ClassifiedError(MessageKey.AI_NO_NETWORK)
        }
        if (
            throwable is SSLException ||
            throwable is CertificateException ||
            throwable is GeneralSecurityException
        ) {
            return ClassifiedError(MessageKey.AI_TLS_ERROR)
        }
        if (throwable is JSONException) {
            return ClassifiedError(MessageKey.AI_PARSE_ERROR)
        }

        val message = throwable.message.orEmpty()
        if (throwable is IllegalArgumentException && message.contains("authorization", ignoreCase = true)) {
            return ClassifiedError(MessageKey.AI_INVALID_API_KEY)
        }
        if (message.contains("unexpected char", ignoreCase = true)) {
            return ClassifiedError(MessageKey.AI_INVALID_API_KEY)
        }
        if (throwable is IOException && message.contains("timeout", ignoreCase = true)) {
            return ClassifiedError(MessageKey.AI_TIMEOUT, listOf(AI_HTTP_READ_TIMEOUT_SECONDS.toString()))
        }
        if (message.contains("unable to resolve host", ignoreCase = true)) {
            return ClassifiedError(MessageKey.AI_NO_NETWORK)
        }
        if (
            message.contains("ssl", ignoreCase = true) ||
            message.contains("handshake", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) ||
            message.contains("trust anchor", ignoreCase = true)
        ) {
            return ClassifiedError(MessageKey.AI_TLS_ERROR)
        }
        if (
            message.contains("connection reset", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("econnrefused", ignoreCase = true)
        ) {
            return ClassifiedError(MessageKey.AI_NO_NETWORK)
        }
        return null
    }

    private fun Throwable.causeChain(): Sequence<Throwable> = generateSequence(this) { it.cause }

    internal class OpenAiException(
        val messageKey: MessageKey,
        val messageArgs: List<String> = emptyList(),
    ) : Exception(messageKey.name)

    private data class ClassifiedError(
        val messageKey: MessageKey,
        val messageArgs: List<String> = emptyList(),
    )
}
