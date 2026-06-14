package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.Tag.Companion.UNTAGGED_NAME
import ua.danichapps.radiantdays.domain.repository.TagRepository

class AddTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(name: String, color: EventColor = EventColor.DEFAULT): DomainResult<Tag> {
        val trimmedName = name.trim()
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
        if (repository.isTagNameTaken(trimmedName)) {
            return DomainResult.Error(
                IllegalArgumentException("Tag name already exists"),
                "Тег с таким именем уже существует",
            )
        }

        return repository.addTag(trimmedName, color)
    }
}
