package ua.danichapps.radiantdays.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.data.local.entity.TagEntity

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY is_pinned DESC, name COLLATE NOCASE ASC")
    fun getTags(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM tags
        WHERE name = :name COLLATE NOCASE
        AND (:excludeGuid IS NULL OR guid != :excludeGuid)
        """
    )
    suspend fun countByName(name: String, excludeGuid: String?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity): Int

    @Query("DELETE FROM tags WHERE guid = :guid")
    suspend fun deleteTag(guid: String): Int
}
