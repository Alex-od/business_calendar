package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
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
                MessageKey.TAG_CANNOT_UPDATE,
            )
        }
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Tag name is required"),
                MessageKey.TAG_NAME_REQUIRED,
            )
        }
        if (trimmedName.equals(UNTAGGED_NAME, ignoreCase = true)) {
            return DomainResult.Error(
                IllegalArgumentException("Reserved tag name"),
                MessageKey.TAG_NAME_RESERVED,
                listOf(UNTAGGED_NAME),
            )
        }
        if (repository.isTagNameTaken(trimmedName, excludeGuid = tag.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Tag name already exists"),
                MessageKey.TAG_NAME_TAKEN,
            )
        }

        return repository.updateTag(tag.copy(name = trimmedName))
    }
}
