package ua.danichapps.radiantdays.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Folder

interface FolderRepository {
    fun getFolders(): Flow<List<Folder>>

    suspend fun isFolderNameTaken(name: String, excludeGuid: String? = null): Boolean

    suspend fun addFolder(name: String): DomainResult<Folder>

    suspend fun updateFolder(folder: Folder): DomainResult<Unit>

    suspend fun deleteFolder(guid: String): DomainResult<Unit>
}
