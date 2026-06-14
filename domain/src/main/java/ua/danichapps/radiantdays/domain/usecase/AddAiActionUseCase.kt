package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class AddAiActionUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        prompt: String,
        isVisible: Boolean = true,
    ): DomainResult<AiAction> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action name is required"),
                "Введите название действия",
            )
        }
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action prompt is required"),
                "Введите промпт",
            )
        }
        if (repository.isActionNameTaken(trimmedName)) {
            return DomainResult.Error(
                IllegalArgumentException("Action name already exists"),
                "Действие с таким названием уже существует",
            )
        }
        val trimmedDescription = description?.trim()?.takeIf { it.isNotBlank() }
        return repository.addAction(trimmedName, trimmedDescription, trimmedPrompt, isVisible)
    }
}
