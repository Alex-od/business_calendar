package ua.danichapps.mybusinesscalendar.domain.model

/**
 * Core business entity representing a calendar event.
 *
 * Uses [Long] (epoch milliseconds) for timestamps to remain framework-agnostic
 * and compatible with future Kotlin Multiplatform migration.
 *
 * @property id           Unique identifier. `0` means the entity has not been persisted yet.
 * @property title        Short, human-readable title of the event.
 * @property description  Optional detailed description.
 * @property startTimeMillis Start of the event in epoch milliseconds (UTC).
 * @property endTimeMillis   End of the event in epoch milliseconds (UTC).
 * @property isAllDay     Whether the event occupies the whole day (time part is ignored).
 * @property color        Visual accent color for UI rendering.
 * @property notificationMinutesBefore Minutes before [startTimeMillis] to trigger a reminder.
 */
data class CalendarEvent(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean = false,
    val color: EventColor = EventColor.DEFAULT,
    val notificationMinutesBefore: Int = 30,
)

/**
 * Available accent colours for calendar events.
 * Kept as a domain enum so the UI layer can translate it to actual colour values.
 */
enum class EventColor {
    DEFAULT, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE
}
