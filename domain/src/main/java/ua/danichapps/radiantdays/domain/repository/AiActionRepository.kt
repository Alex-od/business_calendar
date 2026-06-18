package ua.danichapps.radiantdays.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult

interface AiActionRepository {
    fun getActions(): Flow<List<AiAction>>

    fun getVisibleActions(): Flow<List<AiAction>>

    suspend fun getActionByGuid(guid: String): AiAction?

    suspend fun isActionNameTaken(name: String, excludeGuid: String? = null): Boolean

    suspend fun addAction(
        name: String,
        description: String?,
        prompt: String,
        isVisible: Boolean,
    ): DomainResult<AiAction>

    suspend fun updateAction(action: AiAction): DomainResult<Unit>

    suspend fun deleteAction(guid: String): DomainResult<Unit>

    suspend fun reorderActions(orderedGuids: List<String>): DomainResult<Unit>

    suspend fun ensureBuiltinActions()
}
