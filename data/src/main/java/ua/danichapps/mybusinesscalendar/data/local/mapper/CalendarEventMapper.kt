package ua.danichapps.mybusinesscalendar.data.local.mapper

import ua.danichapps.mybusinesscalendar.data.local.entity.CalendarEventEntity
import ua.danichapps.mybusinesscalendar.data.remote.dto.CalendarEventDto
import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent
import ua.danichapps.mybusinesscalendar.domain.model.EventColor

// ── Entity ↔ Domain ──────────────────────────────────────────────────────────

/** Converts a Room entity to a domain model. */
fun CalendarEventEntity.toDomain(): CalendarEvent = CalendarEvent(
    id                       = id,
    title                    = title,
    description              = description,
    startTimeMillis          = startTimeMillis,
    endTimeMillis            = endTimeMillis,
    isAllDay                 = isAllDay,
    color                    = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
)

/** Converts a domain model to a Room entity ready for persistence. */
fun CalendarEvent.toEntity(): CalendarEventEntity = CalendarEventEntity(
    id                       = id,
    title                    = title,
    description              = description,
    startTimeMillis          = startTimeMillis,
    endTimeMillis            = endTimeMillis,
    isAllDay                 = isAllDay,
    color                    = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
)

// ── DTO ↔ Domain ─────────────────────────────────────────────────────────────

/** Converts a network DTO to a domain model. */
fun CalendarEventDto.toDomain(): CalendarEvent = CalendarEvent(
    id                       = id,
    title                    = title,
    description              = description,
    startTimeMillis          = startTimeMillis,
    endTimeMillis            = endTimeMillis,
    isAllDay                 = isAllDay,
    color                    = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
)

/** Converts a domain model to a network DTO for API calls. */
fun CalendarEvent.toDto(): CalendarEventDto = CalendarEventDto(
    id                       = id,
    title                    = title,
    description              = description,
    startTimeMillis          = startTimeMillis,
    endTimeMillis            = endTimeMillis,
    isAllDay                 = isAllDay,
    color                    = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toEventColor(): EventColor =
    runCatching { EventColor.valueOf(this) }.getOrDefault(EventColor.DEFAULT)
