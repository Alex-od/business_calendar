package ua.danichapps.mybusinesscalendar.domain.usecase

import ua.danichapps.mybusinesscalendar.domain.model.DomainResult
import ua.danichapps.mybusinesscalendar.domain.repository.CalendarEventRepository

/**
 * Deletes a [ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent] by its ID.
 *
 * Kept as a dedicated use-case so that any future pre-/post-deletion logic
 * (e.g. cancelling scheduled notifications) can be added here without touching
 * the ViewModel or repository.
 *
 * @param repository Data source abstraction (injected).
 */
class DeleteEventUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param id The unique identifier of the event to delete.
     * @return [DomainResult.Success] if deleted, [DomainResult.Error] otherwise.
     */
    suspend operator fun invoke(id: Long): DomainResult<Unit> =
        repository.deleteEvent(id)
}
