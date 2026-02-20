package ua.danichapps.mybusinesscalendar.domain.usecase

import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult
import ua.danichapps.mybusinesscalendar.domain.repository.CalendarEventRepository

/**
 * Validates and persists a new [CalendarEvent].
 *
 * Business rules enforced here:
 * - Title must not be blank.
 * - End time must be ≥ start time.
 *
 * The ViewModel remains logic-free; it only passes user input to this use-case
 * and reacts to the returned [DomainResult].
 *
 * @param repository Data source abstraction (injected).
 */
class AddEventUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param event Event to persist. [CalendarEvent.id] is ignored (auto-generated).
     * @return [DomainResult.Success] with the new row ID, or [DomainResult.Error].
     */
    suspend operator fun invoke(event: CalendarEvent): DomainResult<Long> {
        if (event.title.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Title must not be blank"))
        }
        if (event.endTimeMillis < event.startTimeMillis) {
            return DomainResult.Error(IllegalArgumentException("End time must not be before start time"))
        }
        return repository.addEvent(event)
    }
}
