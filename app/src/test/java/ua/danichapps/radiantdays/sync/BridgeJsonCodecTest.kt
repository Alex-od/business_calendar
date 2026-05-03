package ua.danichapps.radiantdays.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.EventColor

class BridgeJsonCodecTest {

    @Test
    fun `parseCommand returns command when payload is valid`() {
        val command = BridgeJsonCodec.parseCommand("""{"action":"get_events","request_id":"req-1"}""")

        assertNotNull(command)
        assertEquals("get_events", command?.action)
        assertEquals("req-1", command?.requestId)
    }

    @Test
    fun `parseCommand returns null for invalid payload`() {
        val command = BridgeJsonCodec.parseCommand("invalid-json")
        assertNull(command)
    }

    @Test
    fun `buildSuccessResponse includes request id and event list`() {
        val payload = BridgeJsonCodec.buildSuccessResponse(
            requestId = "req-2",
            events = listOf(
                CalendarEvent(
                    id = 7L,
                    description = "Daily sync",
                    startTimeMillis = 1_000L,
                    endTimeMillis = 2_000L,
                    isAllDay = false,
                    color = EventColor.BLUE,
                    notificationMinutesBefore = 15,
                    isCompleted = false,
                )
            ),
        )

        val root = Json.parseToJsonElement(payload).jsonObject
        val result = root["result"]?.jsonArray
        val first = result?.firstOrNull()?.jsonObject

        assertEquals("req-2", root["request_id"]?.jsonPrimitive?.content)
        assertEquals(1, result?.size)
        assertEquals(7L, first?.get("id")?.jsonPrimitive?.content?.toLong())
        assertEquals("Daily sync", first?.get("description")?.jsonPrimitive?.content)
        assertEquals("BLUE", first?.get("color")?.jsonPrimitive?.content)
    }
}
