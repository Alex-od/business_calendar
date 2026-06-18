package ua.danichapps.radiantdays.locale

import android.content.Context
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.MessageKey

class DomainErrorStrings(context: Context) {

    private val appContext = context.applicationContext

    fun resolve(key: MessageKey, args: List<String> = emptyList(), cause: Throwable? = null): String {
        if (key == MessageKey.AI_HTTP_ERROR) {
            val code = args.getOrNull(0).orEmpty()
            val detail = args.getOrNull(1).orEmpty().trim()
            val base = appContext.getString(R.string.error_ai_http_error, code)
            return if (detail.isBlank()) base else "$base\n$detail"
        }
        if (key == MessageKey.AI_REQUEST_FAILED) {
            val hint = humanizeAiFailureDetail(args.getOrNull(0).orEmpty())
            return if (hint.isBlank()) {
                appContext.getString(R.string.error_ai_request_failed)
            } else {
                appContext.getString(R.string.error_ai_request_failed_with_detail, hint)
            }
        }
        val resId = when (key) {
            MessageKey.UNKNOWN -> R.string.error_unknown
            MessageKey.TAG_NAME_REQUIRED -> R.string.error_tag_name_required
            MessageKey.TAG_NAME_RESERVED -> R.string.error_tag_name_reserved
            MessageKey.TAG_NAME_TAKEN -> R.string.error_tag_name_taken
            MessageKey.TAG_CANNOT_UPDATE -> R.string.error_tag_cannot_update
            MessageKey.TAG_CANNOT_DELETE -> R.string.error_tag_cannot_delete
            MessageKey.AI_ACTION_NAME_REQUIRED -> R.string.error_ai_action_name_required
            MessageKey.AI_ACTION_PROMPT_REQUIRED -> R.string.error_ai_action_prompt_required
            MessageKey.AI_ACTION_NAME_TAKEN -> R.string.error_ai_action_name_taken
            MessageKey.AI_ACTION_INVALID -> R.string.error_ai_action_invalid
            MessageKey.AI_ACTION_BUILTIN_DELETE -> R.string.error_ai_action_builtin_delete
            MessageKey.AI_ACTION_ORDER_EMPTY -> R.string.error_ai_action_order_empty
            MessageKey.AI_ACTION_NOT_FOUND -> R.string.error_ai_action_not_found
            MessageKey.NOTE_TEXT_REQUIRED -> R.string.error_note_text_required
            MessageKey.CHAT_MESSAGE_REQUIRED -> R.string.error_chat_message_required
            MessageKey.EVENT_TEXT_BLANK -> R.string.error_event_text_blank
            MessageKey.EVENT_END_BEFORE_START -> R.string.error_event_end_before_start
            MessageKey.EVENT_UNSAVED_UPDATE -> R.string.error_event_unsaved_update
            MessageKey.LOAD_TAGS_FAILED -> R.string.error_load_tags_failed
            MessageKey.LOAD_AI_ACTIONS_FAILED -> R.string.error_load_ai_actions_failed
            MessageKey.LOAD_NOTES_FAILED -> R.string.error_load_notes_failed
            MessageKey.NOTE_NOT_FOUND -> R.string.error_note_not_found
            MessageKey.AI_INVALID_API_KEY -> R.string.error_ai_invalid_api_key
            MessageKey.AI_HTTP_ERROR -> R.string.error_ai_http_error
            MessageKey.AI_EMPTY_RESPONSE -> R.string.error_ai_empty_response
            MessageKey.AI_TIMEOUT -> R.string.error_ai_timeout
            MessageKey.AI_REQUEST_FAILED -> R.string.error_ai_request_failed
            MessageKey.AI_NO_NETWORK -> R.string.error_ai_no_network
            MessageKey.AI_MODEL_NOT_FOUND -> R.string.error_ai_model_not_found
            MessageKey.AI_RATE_LIMIT -> R.string.error_ai_rate_limit
            MessageKey.AI_SERVER_ERROR -> R.string.error_ai_server_error
            MessageKey.AI_PARSE_ERROR -> R.string.error_ai_parse_error
            MessageKey.AI_TLS_ERROR -> R.string.error_ai_tls_error
        }
        val effectiveArgs = when (key) {
            MessageKey.AI_HTTP_ERROR -> httpErrorArgs(args)
            else -> args
        }
        return if (effectiveArgs.isEmpty()) {
            appContext.getString(resId)
        } else {
            appContext.getString(resId, *effectiveArgs.toTypedArray())
        }
    }

    private fun httpErrorArgs(args: List<String>): List<String> {
        val code = args.getOrNull(0).orEmpty()
        val detail = args.getOrNull(1).orEmpty()
        if (detail.isBlank()) return listOf(code)
        return listOf(code, detail)
    }

    private fun humanizeAiFailureDetail(raw: String): String {
        val detail = raw.trim()
        if (detail.isBlank()) return ""
        val lower = detail.lowercase()
        return when {
            lower.contains("unexpected char") || lower.contains("authorization value") ->
                appContext.getString(R.string.error_ai_invalid_api_key)
            lower.contains("trust anchor") || lower.contains("cert path") || lower.contains("certificate") ->
                appContext.getString(R.string.error_ai_hint_certificate)
            lower.contains("handshake") || lower.contains("ssl") ->
                appContext.getString(R.string.error_ai_hint_ssl)
            lower.contains("unable to resolve host") || lower.contains("unknown host") ->
                appContext.getString(R.string.error_ai_hint_dns)
            lower.contains("timeout") || lower.contains("timed out") ->
                appContext.getString(R.string.error_ai_hint_timeout)
            lower.contains("connection reset") ||
                lower.contains("failed to connect") ||
                lower.contains("econnrefused") ->
                appContext.getString(R.string.error_ai_hint_connection)
            else -> appContext.getString(R.string.error_ai_hint_technical, detail.take(120))
        }
    }
}
