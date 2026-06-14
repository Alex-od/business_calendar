package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.repository.TagRepository

class DeleteTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tagGuid: String): DomainResult<Unit> {
        if (Tag.isUntaggedFilter(tagGuid)) {
            return DomainResult.Error(
                IllegalArgumentException("Cannot delete virtual tag"),
                "Этот тег нельзя удалить",
            )
        }
        return repository.deleteTag(tagGuid)
    }
}
