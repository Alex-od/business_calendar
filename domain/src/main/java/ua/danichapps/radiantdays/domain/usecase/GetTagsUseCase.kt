package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.repository.TagRepository

class GetTagsUseCase(
    private val repository: TagRepository,
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getTags()
}
