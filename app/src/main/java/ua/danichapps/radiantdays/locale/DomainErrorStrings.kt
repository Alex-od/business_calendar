package ua.danichapps.radiantdays.locale

import android.content.Context
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.MessageKey

class DomainErrorStrings(context: Context) {

    private val appContext = context.applicationContext

    fun resolve(key: MessageKey, args: List<String> = emptyList()): String {
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
        }
        return if (args.isEmpty()) {
            appContext.getString(resId)
        } else {
            appContext.getString(resId, *args.toTypedArray())
        }
    }
}
