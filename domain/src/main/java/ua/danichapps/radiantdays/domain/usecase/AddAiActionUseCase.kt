package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
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
                MessageKey.AI_ACTION_NAME_REQUIRED,
            )
        }
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action prompt is required"),
                MessageKey.AI_ACTION_PROMPT_REQUIRED,
            )
        }
        if (repository.isActionNameTaken(trimmedName)) {
            return DomainResult.Error(
                IllegalArgumentException("Action name already exists"),
                MessageKey.AI_ACTION_NAME_TAKEN,
            )
        }
        val trimmedDescription = description?.trim()?.takeIf { it.isNotBlank() }
        return repository.addAction(trimmedName, trimmedDescription, trimmedPrompt, isVisible)
    }
}
