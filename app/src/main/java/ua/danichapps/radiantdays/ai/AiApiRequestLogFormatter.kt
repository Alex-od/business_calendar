package ua.danichapps.radiantdays.ai

import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object AiApiRequestLogFormatter {

    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT

    fun formatHttp(
        source: AiApiRequestSource,
        request: Request,
        requestBody: String,
        response: Response,
        responseBody: String,
    ): String = buildString {
        appendHeader(source)
        appendRequestSection(request, requestBody)
        appendLine()
        appendLine("=== RESPONSE ===")
        appendLine("HTTP ${response.code}")
        appendLine()
        appendLine("Headers:")
        appendHeaders(response.headers)
        appendLine()
        appendLine("Body:")
        appendLine(prettyJsonOrRaw(responseBody))
    }

    fun formatNetworkError(
        source: AiApiRequestSource,
        request: Request,
        requestBody: String,
        error: Throwable,
    ): String = buildString {
        appendHeader(source)
        appendRequestSection(request, requestBody)
        appendLine()
        appendLine("=== ERROR ===")
        appendLine(error.toLogLine())
    }

    private fun StringBuilder.appendHeader(source: AiApiRequestSource) {
        appendLine(timestampFormatter.format(Instant.now().atOffset(ZoneOffset.UTC)))
        appendLine("Source: $source")
        appendLine()
    }

    private fun StringBuilder.appendRequestSection(request: Request, requestBody: String) {
        appendLine("=== REQUEST ===")
        appendLine("${request.method} ${request.url}")
        appendLine()
        appendLine("Headers:")
        appendHeaders(request.headers)
        appendLine()
        appendLine("Body:")
        appendLine(prettyJsonOrRaw(requestBody))
    }

    private fun StringBuilder.appendHeaders(headers: Headers) {
        if (headers.size == 0) {
            appendLine("(none)")
            return
        }
        for (index in 0 until headers.size) {
            appendLine("${headers.name(index)}: ${headers.value(index)}")
        }
    }

    private fun prettyJsonOrRaw(text: String): String {
        if (text.isBlank()) return text
        return runCatching {
            when {
                text.trimStart().startsWith("{") -> JSONObject(text).toString(2)
                text.trimStart().startsWith("[") -> JSONArray(text).toString(2)
                else -> text
            }
        }.getOrDefault(text)
    }

    private fun Throwable.toLogLine(): String =
        "${javaClass.name}: ${message.orEmpty()}"
}
