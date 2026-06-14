package ua.danichapps.radiantdays.data.local.mapper

import ua.danichapps.radiantdays.data.local.entity.AiActionEntity
import ua.danichapps.radiantdays.domain.model.AiAction

fun AiActionEntity.toDomain(): AiAction = AiAction(
    guid = guid,
    name = name,
    description = description,
    prompt = prompt,
    isVisible = isVisible,
    sortOrder = sortOrder,
    isBuiltIn = isBuiltIn,
)

fun AiAction.toEntity(): AiActionEntity = AiActionEntity(
    guid = guid,
    name = name,
    description = description,
    prompt = prompt,
    isVisible = isVisible,
    sortOrder = sortOrder,
    isBuiltIn = isBuiltIn,
)
