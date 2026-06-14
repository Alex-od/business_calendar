package ua.danichapps.radiantdays.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.danichapps.radiantdays.data.local.dao.AiActionDao
import ua.danichapps.radiantdays.data.local.entity.AiActionEntity
import ua.danichapps.radiantdays.data.local.mapper.toDomain
import ua.danichapps.radiantdays.data.local.mapper.toEntity
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository
import java.util.UUID

class AiActionRepositoryImpl(
    private val dao: AiActionDao,
) : AiActionRepository {

    override fun getActions(): Flow<List<AiAction>> =
        dao.getActions().map { entities -> entities.map { it.toDomain() } }

    override fun getVisibleActions(): Flow<List<AiAction>> =
        dao.getVisibleActions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getActionByGuid(guid: String): AiAction? =
        dao.getByGuid(guid)?.toDomain()

    override suspend fun isActionNameTaken(name: String, excludeGuid: String?): Boolean =
        dao.countByName(name = name, excludeGuid = excludeGuid) > 0

    override suspend fun addAction(
        name: String,
        description: String?,
        prompt: String,
        isVisible: Boolean,
    ): DomainResult<AiAction> =
        runCatching {
            val sortOrder = dao.countAll()
            val entity = AiActionEntity(
                guid = UUID.randomUUID().toString(),
                name = name,
                description = description,
                prompt = prompt,
                isVisible = isVisible,
                sortOrder = sortOrder,
                isBuiltIn = false,
            )
            dao.insertAction(entity)
            entity.toDomain()
        }.fold(
            onSuccess = { DomainResult.Success(it) },
            onFailure = { DomainResult.Error(it) },
        )

    override suspend fun updateAction(action: AiAction): DomainResult<Unit> =
        runCatching { dao.updateAction(action.toEntity()) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun deleteAction(guid: String): DomainResult<Unit> =
        runCatching { dao.deleteAction(guid) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = { DomainResult.Error(it) },
            )

    override suspend fun reorderActions(orderedGuids: List<String>): DomainResult<Unit> =
        runCatching {
            val entities = orderedGuids.mapIndexed { index, guid ->
                val entity = dao.getByGuid(guid)
                    ?: throw IllegalArgumentException("Unknown action guid: $guid")
                entity.copy(sortOrder = index)
            }
            dao.reorderActions(entities)
        }.fold(
            onSuccess = { DomainResult.Success(Unit) },
            onFailure = { DomainResult.Error(it) },
        )
}
