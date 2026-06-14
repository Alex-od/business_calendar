package ua.danichapps.radiantdays.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ua.danichapps.radiantdays.data.local.entity.NoteTagCrossRef

@Dao
interface NoteTagDao {
    @Query("DELETE FROM note_tags WHERE note_id = :noteId")
    suspend fun deleteForNote(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<NoteTagCrossRef>)

    @Transaction
    suspend fun replaceTagsForNote(noteId: Long, tagGuids: Set<String>) {
        deleteForNote(noteId)
        if (tagGuids.isNotEmpty()) {
            insertAll(tagGuids.map { NoteTagCrossRef(noteId = noteId, tagGuid = it) })
        }
    }
}
