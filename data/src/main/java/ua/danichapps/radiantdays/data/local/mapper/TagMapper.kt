package ua.danichapps.radiantdays.data.local.mapper

import ua.danichapps.radiantdays.data.local.entity.TagEntity
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(
    guid = guid,
    name = name,
    isPinned = isPinned,
    color = color.toEventColor(),
)

fun Tag.toEntity(): TagEntity = TagEntity(
    guid = guid,
    name = name,
    isPinned = isPinned,
    color = color.name,
)

private fun String.toEventColor(): EventColor =
    runCatching { EventColor.valueOf(this) }.getOrDefault(EventColor.DEFAULT)
