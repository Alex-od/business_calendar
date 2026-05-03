package ua.danichapps.radiantdays.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.data.local.entity.FolderEntity

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun getFolders(): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM folders
        WHERE name = :name COLLATE NOCASE
        AND (:excludeGuid IS NULL OR guid != :excludeGuid)
        """
    )
    suspend fun countByName(name: String, excludeGuid: String?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Query("DELETE FROM folders WHERE guid = :guid")
    suspend fun deleteFolder(guid: String): Int
}
