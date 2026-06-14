package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class UpdateAiActionUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(action: AiAction): DomainResult<Unit> {
        if (action.guid.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action guid is required"),
                "Некорректное действие",
            )
        }
        val trimmedName = action.name.trim()
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action name is required"),
                "Введите название действия",
            )
        }
        val trimmedPrompt = action.prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action prompt is required"),
                "Введите промпт",
            )
        }
        if (repository.isActionNameTaken(trimmedName, excludeGuid = action.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Action name already exists"),
                "Действие с таким названием уже существует",
            )
        }
        val trimmedDescription = action.description?.trim()?.takeIf { it.isNotBlank() }
        return repository.updateAction(
            action.copy(
                name = trimmedName,
                description = trimmedDescription,
                prompt = trimmedPrompt,
            ),
        )
    }
}
