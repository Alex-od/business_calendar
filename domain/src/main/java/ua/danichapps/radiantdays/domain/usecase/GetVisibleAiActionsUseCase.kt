package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class GetVisibleAiActionsUseCase(
    private val repository: AiActionRepository,
) {
    operator fun invoke(): Flow<List<AiAction>> = repository.getVisibleActions()
}
