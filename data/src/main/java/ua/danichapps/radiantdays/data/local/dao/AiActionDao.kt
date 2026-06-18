package ua.danichapps.radiantdays.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.data.local.entity.AiActionEntity

@Dao
interface AiActionDao {
    @Query("SELECT * FROM ai_actions ORDER BY sort_order ASC")
    fun getActions(): Flow<List<AiActionEntity>>

    @Query("SELECT * FROM ai_actions WHERE is_visible = 1 ORDER BY sort_order ASC")
    fun getVisibleActions(): Flow<List<AiActionEntity>>

    @Query("SELECT * FROM ai_actions WHERE guid = :guid LIMIT 1")
    suspend fun getByGuid(guid: String): AiActionEntity?

    @Query("SELECT COUNT(*) FROM ai_actions")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT COUNT(*) FROM ai_actions
        WHERE name = :name COLLATE NOCASE
        AND (:excludeGuid IS NULL OR guid != :excludeGuid)
        """
    )
    suspend fun countByName(name: String, excludeGuid: String?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAction(action: AiActionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActions(actions: List<AiActionEntity>)

    @Update
    suspend fun updateAction(action: AiActionEntity): Int

    @Query("DELETE FROM ai_actions WHERE guid = :guid")
    suspend fun deleteAction(guid: String): Int

    @Transaction
    suspend fun reorderActions(entities: List<AiActionEntity>) {
        entities.forEach { updateAction(it) }
    }
}
