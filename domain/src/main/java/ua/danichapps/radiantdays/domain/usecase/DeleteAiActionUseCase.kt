package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class DeleteAiActionUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(guid: String): DomainResult<Unit> {
        val action = repository.getActionByGuid(guid)
        if (action?.isBuiltIn == true) {
            return DomainResult.Error(
                IllegalArgumentException("Built-in action cannot be deleted"),
                MessageKey.AI_ACTION_BUILTIN_DELETE,
            )
        }
        return repository.deleteAction(guid)
    }
}
