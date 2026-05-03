package ua.danichapps.radiantdays.data.local.mapper

import ua.danichapps.radiantdays.data.local.entity.NoteEntity
import ua.danichapps.radiantdays.data.remote.dto.CalendarEventDto
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.EventColor

// в”Ђв”Ђ Entity в†” Domain в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

/** Converts a Room entity to a domain model. */
fun NoteEntity.toDomain(): CalendarEvent = CalendarEvent(
    id                        = id,
    description               = description,
    startTimeMillis           = startTimeMillis,
    endTimeMillis             = endTimeMillis,
    isAllDay                  = isAllDay,
    color                     = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
    isCompleted               = isCompleted,
    folderGuid                = folderGuid,
)

/** Converts a domain model to a Room entity ready for persistence. */
fun CalendarEvent.toEntity(): NoteEntity = NoteEntity(
    id                        = id,
    description               = description,
    startTimeMillis           = startTimeMillis,
    endTimeMillis             = endTimeMillis,
    isAllDay                  = isAllDay,
    color                     = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
    isCompleted               = isCompleted,
    folderGuid                = folderGuid,
)

// в”Ђв”Ђ DTO в†” Domain в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

/** Converts a network DTO to a domain model. */
fun CalendarEventDto.toDomain(): CalendarEvent = CalendarEvent(
    id                        = id,
    description               = description,
    startTimeMillis           = startTimeMillis,
    endTimeMillis             = endTimeMillis,
    isAllDay                  = isAllDay,
    color                     = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
    isCompleted               = isCompleted,
    folderGuid                = folderGuid,
)

/** Converts a domain model to a network DTO for API calls. */
fun CalendarEvent.toDto(): CalendarEventDto = CalendarEventDto(
    id                        = id,
    description               = description,
    startTimeMillis           = startTimeMillis,
    endTimeMillis             = endTimeMillis,
    isAllDay                  = isAllDay,
    color                     = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
    isCompleted               = isCompleted,
    folderGuid                = folderGuid,
)

// в”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

private fun String.toEventColor(): EventColor =
    runCatching { EventColor.valueOf(this) }.getOrDefault(EventColor.DEFAULT)
