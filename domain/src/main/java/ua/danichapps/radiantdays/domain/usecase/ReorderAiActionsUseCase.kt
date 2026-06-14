package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class ReorderAiActionsUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(orderedGuids: List<String>): DomainResult<Unit> {
        if (orderedGuids.isEmpty()) {
            return DomainResult.Error(
                IllegalArgumentException("Order list is empty"),
                "Список действий пуст",
            )
        }
        return repository.reorderActions(orderedGuids)
    }
}
