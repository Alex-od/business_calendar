package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.AiNoteContext
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.domain.util.AiPromptTemplate

class RunAiActionUseCase(
    private val repository: AiActionRepository,
    private val clientProvider: AiCompletionClientProvider,
) {
    suspend operator fun invoke(actionGuid: String, context: AiNoteContext): DomainResult<String> {
        if (context.text.trim().isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Note text is required"),
                "Введите текст заметки",
            )
        }
        val action = repository.getActionByGuid(actionGuid)
            ?: return DomainResult.Error(
                IllegalArgumentException("Action not found"),
                "Действие не найдено",
            )
        val resolvedPrompt = AiPromptTemplate.resolve(action.prompt, context)
        return clientProvider.getClient().complete(resolvedPrompt)
    }
}
