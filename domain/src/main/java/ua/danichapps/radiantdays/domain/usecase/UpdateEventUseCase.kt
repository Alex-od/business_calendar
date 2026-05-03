package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/**
 * Validates and updates an existing [CalendarEvent].
 *
 * Business rules enforced here:
 * - [CalendarEvent.id] must be a positive non-zero value.
 * - Description must not be blank.
 * - End time must be в‰Ґ start time.
 *
 * @param repository Data source abstraction (injected).
 */
class UpdateEventUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param event Updated event. Must have a valid [CalendarEvent.id].
     * @return [DomainResult.Success] on success, [DomainResult.Error] on validation/storage failure.
     */
    suspend operator fun invoke(event: CalendarEvent): DomainResult<Unit> {
        if (event.id == 0L) {
            return DomainResult.Error(IllegalArgumentException("Cannot update an event with id=0"))
        }
        if (event.description.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Description must not be blank"))
        }
        if (event.endTimeMillis < event.startTimeMillis) {
            return DomainResult.Error(IllegalArgumentException("End time must not be before start time"))
        }
        return repository.updateEvent(event)
    }
}
