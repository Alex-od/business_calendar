package ua.danichapps.radiantdays.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.danichapps.radiantdays.data.local.dao.TagDao
import ua.danichapps.radiantdays.data.local.entity.TagEntity
import ua.danichapps.radiantdays.data.local.mapper.toDomain
import ua.danichapps.radiantdays.data.local.mapper.toEntity
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.repository.TagRepository
import java.util.UUID

class TagRepositoryImpl(
    private val dao: TagDao,
) : TagRepository {

    override fun getTags(): Flow<List<Tag>> =
        dao.getTags().map { entities -> entities.map { it.toDomain() } }

    override suspend fun isTagNameTaken(name: String, excludeGuid: String?): Boolean =
        dao.countByName(name = name, excludeGuid = excludeGuid) > 0

    override suspend fun addTag(name: String, color: EventColor): DomainResult<Tag> =
        runCatching {
            val tag = TagEntity(
                guid = UUID.randomUUID().toString(),
                name = name,
                color = color.name,
            )
            dao.insertTag(tag)
            tag.toDomain()
        }.fold(
            onSuccess = { DomainResult.Success(it) },
            onFailure = { DomainResult.Error(it) },
        )

    override suspend fun updateTag(tag: Tag): DomainResult<Unit> =
        runCatching { dao.updateTag(tag.toEntity()) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun deleteTag(guid: String): DomainResult<Unit> =
        runCatching { dao.deleteTag(guid) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )
}
