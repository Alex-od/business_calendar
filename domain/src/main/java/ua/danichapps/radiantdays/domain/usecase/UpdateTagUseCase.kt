package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.Tag.Companion.UNTAGGED_NAME
import ua.danichapps.radiantdays.domain.repository.TagRepository

class UpdateTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tag: Tag): DomainResult<Unit> {
        val trimmedName = tag.name.trim()
        if (tag.guid.isBlank() || Tag.isUntaggedFilter(tag.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Cannot update virtual tag"),
                "Этот тег нельзя изменить",
            )
        }
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Tag name is required"),
                "Введите имя тега",
            )
        }
        if (trimmedName.equals(UNTAGGED_NAME, ignoreCase = true)) {
            return DomainResult.Error(
                IllegalArgumentException("Reserved tag name"),
                "Имя «$UNTAGGED_NAME» зарезервировано",
            )
        }
        if (repository.isTagNameTaken(trimmedName, excludeGuid = tag.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Tag name already exists"),
                "Тег с таким именем уже существует",
            )
        }

        return repository.updateTag(tag.copy(name = trimmedName))
    }
}
