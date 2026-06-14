package ua.danichapps.radiantdays.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag

interface TagRepository {
    fun getTags(): Flow<List<Tag>>

    suspend fun isTagNameTaken(name: String, excludeGuid: String? = null): Boolean

    suspend fun addTag(name: String, color: EventColor = EventColor.DEFAULT): DomainResult<Tag>

    suspend fun updateTag(tag: Tag): DomainResult<Unit>

    suspend fun deleteTag(guid: String): DomainResult<Unit>
}
