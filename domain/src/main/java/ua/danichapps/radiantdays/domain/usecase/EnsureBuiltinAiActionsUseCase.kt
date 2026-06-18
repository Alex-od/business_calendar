package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.repository.AiActionRepository

class EnsureBuiltinAiActionsUseCase(
    private val repository: AiActionRepository,
) {
    suspend operator fun invoke() {
        repository.ensureBuiltinActions()
    }
}
