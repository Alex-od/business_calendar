package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiActionResult
import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.domain.util.AiPromptTemplate

class RunAiActionUseCase(
    private val repository: AiActionRepository,
    private val clientProvider: AiCompletionClientProvider,
) {
    suspend operator fun invoke(actionGuid: String, context: AiNoteContext): DomainResult<AiActionResult> {
        if (context.text.trim().isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Note text is required"),
                MessageKey.NOTE_TEXT_REQUIRED,
            )
        }
        val action = repository.getActionByGuid(actionGuid)
            ?: return DomainResult.Error(
                IllegalArgumentException("Action not found"),
                MessageKey.AI_ACTION_NOT_FOUND,
            )
        val resolvedPrompt = AiPromptTemplate.resolve(action.prompt, context)
        return when (val result = clientProvider.getClient().complete(resolvedPrompt)) {
            is DomainResult.Success -> DomainResult.Success(
                AiActionResult(resolvedPrompt = resolvedPrompt, response = result.data),
            )
            is DomainResult.Error -> result
        }
    }
}
