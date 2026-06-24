package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.danichapps.radiantdays.domain.localization.AiActionLocalizer
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class GetVisibleAiActionsUseCase(
    private val repository: AiActionRepository,
    private val localizer: AiActionLocalizer,
) {
    operator fun invoke(): Flow<List<AiAction>> =
        repository.getVisibleActions().map { actions -> actions.map(localizer::localize) }
}
