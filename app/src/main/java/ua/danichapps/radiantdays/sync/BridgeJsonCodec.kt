package ua.danichapps.radiantdays.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.danichapps.radiantdays.domain.model.CalendarEvent

internal data class BridgeCommand(
    val action: String,
    val requestId: String,
)

internal object BridgeJsonCodec {

    fun parseCommand(raw: String): BridgeCommand? {
        val root = parseJsonObject(raw) ?: return null
        val action = root["action"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val requestId = root["request_id"]?.jsonPrimitive?.contentOrNull.orEmpty()

        return if (action.isBlank() || requestId.isBlank()) {
            null
        } else {
            BridgeCommand(action = action, requestId = requestId)
        }
    }

    fun buildSuccessResponse(requestId: String, events: List<CalendarEvent>): String {
        val result = buildJsonArray {
            events.forEach { event ->
                add(
                    buildJsonObject {
                        put("id", JsonPrimitive(event.id))
                        put("description", JsonPrimitive(event.description))
                        put("start_time_millis", JsonPrimitive(event.startTimeMillis))
                        put("end_time_millis", JsonPrimitive(event.endTimeMillis))
                        put("is_all_day", JsonPrimitive(event.isAllDay))
                        put("color", JsonPrimitive(event.color.name))
                        put("notification_minutes_before", JsonPrimitive(event.notificationMinutesBefore))
                        event.alarmTimeMillis?.let { alarmTimeMillis ->
                            put("alarm_time_millis", JsonPrimitive(alarmTimeMillis))
                        }
                        put("is_completed", JsonPrimitive(event.isCompleted))
                    }
                )
            }
        }

        return buildJsonObject {
            put("request_id", JsonPrimitive(requestId))
            put("result", result)
        }.toString()
    }

    fun buildErrorResponse(requestId: String, message: String): String =
        buildJsonObject {
            put("request_id", JsonPrimitive(requestId))
            put("error", JsonPrimitive(message))
        }.toString()

    private fun parseJsonObject(raw: String): JsonObject? {
        val element = runCatching { Json.parseToJsonElement(raw) }.getOrNull() ?: return null
        return element as? JsonObject
    }

    private val JsonPrimitive.contentOrNull: String?
        get() = runCatching { content }.getOrNull()
}
