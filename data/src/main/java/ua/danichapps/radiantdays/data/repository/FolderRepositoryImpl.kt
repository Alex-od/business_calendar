package ua.danichapps.radiantdays.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ua.danichapps.radiantdays.data.local.dao.FolderDao
import ua.danichapps.radiantdays.data.local.entity.FolderEntity
import ua.danichapps.radiantdays.data.local.mapper.toDomain
import ua.danichapps.radiantdays.data.local.mapper.toEntity
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Folder
import ua.danichapps.radiantdays.domain.repository.FolderRepository
import java.util.UUID

class FolderRepositoryImpl(
    private val dao: FolderDao,
) : FolderRepository {

    override fun getFolders(): Flow<List<Folder>> =
        dao.getFolders()
            .onEach { folders ->
                Log.d("qqwe_tag FolderRepositoryImpl, getFolders", "READ: count:${folders.size}")
            }
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun isFolderNameTaken(name: String, excludeGuid: String?): Boolean {
        val count = dao.countByName(name = name, excludeGuid = excludeGuid)
        Log.d(
            "qqwe_tag FolderRepositoryImpl, isFolderNameTaken",
            "READ: name:$name, excludeGuid:$excludeGuid, count:$count",
        )
        return count > 0
    }

    override suspend fun addFolder(name: String): DomainResult<Folder> =
        runCatching {
            val folder = FolderEntity(
                guid = UUID.randomUUID().toString(),
                name = name,
            )
            Log.d("qqwe_tag FolderRepositoryImpl, addFolder", "WRITE: folder:$folder")
            dao.insertFolder(folder)
            folder.toDomain()
        }.fold(
            onSuccess = { folder -> DomainResult.Success(folder) },
            onFailure = { DomainResult.Error(it) },
        )

    override suspend fun updateFolder(folder: Folder): DomainResult<Unit> =
        runCatching {
            Log.d("qqwe_tag FolderRepositoryImpl, updateFolder", "WRITE: folder:$folder")
            dao.updateFolder(folder.toEntity())
        }.fold(
            onSuccess = { DomainResult.Success(Unit) },
            onFailure = { DomainResult.Error(it) },
        )

    override suspend fun deleteFolder(guid: String): DomainResult<Unit> =
        runCatching {
            Log.d("qqwe_tag FolderRepositoryImpl, deleteFolder", "WRITE: guid:$guid")
            dao.deleteFolder(guid)
        }.fold(
            onSuccess = { DomainResult.Success(Unit) },
            onFailure = { DomainResult.Error(it) },
        )
}
