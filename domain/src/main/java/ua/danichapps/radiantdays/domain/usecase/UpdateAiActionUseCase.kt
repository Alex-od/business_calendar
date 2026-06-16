package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class UpdateAiActionUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(action: AiAction): DomainResult<Unit> {
        if (action.guid.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action guid is required"),
                MessageKey.AI_ACTION_INVALID,
            )
        }
        val trimmedName = action.name.trim()
        if (trimmedName.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action name is required"),
                MessageKey.AI_ACTION_NAME_REQUIRED,
            )
        }
        val trimmedPrompt = action.prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Action prompt is required"),
                MessageKey.AI_ACTION_PROMPT_REQUIRED,
            )
        }
        if (repository.isActionNameTaken(trimmedName, excludeGuid = action.guid)) {
            return DomainResult.Error(
                IllegalArgumentException("Action name already exists"),
                MessageKey.AI_ACTION_NAME_TAKEN,
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
