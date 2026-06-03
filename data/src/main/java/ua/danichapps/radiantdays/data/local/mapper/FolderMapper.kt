package ua.danichapps.radiantdays.data.local.mapper

import ua.danichapps.radiantdays.data.local.entity.FolderEntity
import ua.danichapps.radiantdays.domain.model.Folder

fun FolderEntity.toDomain(): Folder = Folder(
    guid = guid,
    name = name,
    isPinned = isPinned,
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    guid = guid,
    name = name,
    isPinned = isPinned,
)
