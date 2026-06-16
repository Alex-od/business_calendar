package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class ReorderAiActionsUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke(orderedGuids: List<String>): DomainResult<Unit> {
        if (orderedGuids.isEmpty()) {
            return DomainResult.Error(
                IllegalArgumentException("Order list is empty"),
                MessageKey.AI_ACTION_ORDER_EMPTY,
            )
        }
        return repository.reorderActions(orderedGuids)
    }
}
