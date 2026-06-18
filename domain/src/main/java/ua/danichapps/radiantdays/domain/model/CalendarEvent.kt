package ua.danichapps.radiantdays.domain.model

/**
 * Core business entity representing a calendar event.
 *
 * Uses [Long] (epoch milliseconds) for timestamps to remain framework-agnostic
 * and compatible with future Kotlin Multiplatform migration.
 *
 * @property id           Unique identifier. `0` means the entity has not been persisted yet.
 * @property title        Short note title shown in the toolbar and lists.
 * @property description  Main note body text.
 * @property startTimeMillis Start of the event in epoch milliseconds (UTC).
 * @property endTimeMillis   End of the event in epoch milliseconds (UTC).
 * @property isAllDay     Whether the event occupies the whole day (time part is ignored).
 * @property color        Visual accent color for UI rendering.
 * @property alarmTimeMillis Optional reminder anchor time. `null` means the note has no alarm.
 * @property notificationMinutesBefore Minutes before [alarmTimeMillis] to trigger a reminder.
 *   Ignored when [alarmTimeMillis] is `null`. Effective fire time = alarmTimeMillis - this offset.
 * @property tagGuids     GUIDs of tags assigned to this note (may be empty).
 * @property aiChatMessages Persisted AI chat history. Non-empty means the note opens in chat mode.
 * @property createdAtMillis Epoch ms when the note was first persisted.
 * @property updatedAtMillis Epoch ms when the note was last modified.
 */
data class CalendarEvent(
    val id: Long = 0L,
    val title: String = "",
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean = false,
    val color: EventColor = EventColor.DEFAULT,
    val notificationMinutesBefore: Int = 30,
    val alarmTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val tagGuids: Set<String> = emptySet(),
    val aiChatMessages: List<AiChatMessage> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

/**
 * Available accent colours for calendar events.
 * Kept as a domain enum so the UI layer can translate it to actual colour values.
 */
enum class EventColor {
    DEFAULT, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE
}

/** Headline for lists and notifications. */
fun CalendarEvent.displayHeadline(): String = title.ifBlank { description }
