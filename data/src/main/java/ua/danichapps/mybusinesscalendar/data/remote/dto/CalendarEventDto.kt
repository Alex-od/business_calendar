package ua.danichapps.mybusinesscalendar.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network Data Transfer Object for a calendar event.
 *
 * Completely decoupled from the domain model: any change in the API contract
 * only affects this file and its mapper, leaving business logic untouched.
 */
@Serializable
data class CalendarEventDto(
    @SerialName("id")                         val id: Long,
    @SerialName("title")                      val title: String,
    @SerialName("description")                val description: String,
    @SerialName("start_time_millis")          val startTimeMillis: Long,
    @SerialName("end_time_millis")            val endTimeMillis: Long,
    @SerialName("is_all_day")                 val isAllDay: Boolean,
    @SerialName("color")                      val color: String,
    @SerialName("notification_minutes_before") val notificationMinutesBefore: Int,
)
