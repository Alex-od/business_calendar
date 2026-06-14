package ua.danichapps.radiantdays.data.local.mapper

import ua.danichapps.radiantdays.data.local.entity.NoteEntity
import ua.danichapps.radiantdays.data.local.entity.NoteWithTags
import ua.danichapps.radiantdays.data.remote.dto.CalendarEventDto
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.EventColor

fun NoteWithTags.toDomain(): CalendarEvent = note.toDomain(
    tagGuids = tags.map { it.guid }.toSet(),
)

fun NoteEntity.toDomain(tagGuids: Set<String> = emptySet()): CalendarEvent = CalendarEvent(
    id = id,
    title = title,
    description = description,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    isAllDay = isAllDay,
    color = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
    alarmTimeMillis = alarmTimeMillis,
    isCompleted = isCompleted,
    tagGuids = tagGuids,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun CalendarEvent.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    description = description,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    isAllDay = isAllDay,
    color = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
    alarmTimeMillis = alarmTimeMillis,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun CalendarEventDto.toDomain(): CalendarEvent = CalendarEvent(
    id = id,
    title = title,
    description = description,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    isAllDay = isAllDay,
    color = color.toEventColor(),
    notificationMinutesBefore = notificationMinutesBefore,
    alarmTimeMillis = alarmTimeMillis,
    isCompleted = isCompleted,
    tagGuids = tagGuids.toSet(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun CalendarEvent.toDto(): CalendarEventDto = CalendarEventDto(
    id = id,
    title = title,
    description = description,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    isAllDay = isAllDay,
    color = color.name,
    notificationMinutesBefore = notificationMinutesBefore,
    alarmTimeMillis = alarmTimeMillis,
    isCompleted = isCompleted,
    tagGuids = tagGuids.toList(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun String.toEventColor(): EventColor =
    runCatching { EventColor.valueOf(this) }.getOrDefault(EventColor.DEFAULT)
