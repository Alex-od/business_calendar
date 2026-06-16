package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class UpdateEventUseCase(private val repository: CalendarEventRepository) {

    suspend operator fun invoke(event: CalendarEvent): DomainResult<Unit> {
        if (event.id == 0L) {
            return DomainResult.Error(
                IllegalArgumentException("Cannot update an event with id=0"),
                MessageKey.EVENT_UNSAVED_UPDATE,
            )
        }
        if (event.title.isBlank() && event.description.isBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("Title or note text must not be blank"),
                MessageKey.EVENT_TEXT_BLANK,
            )
        }
        if (event.endTimeMillis < event.startTimeMillis) {
            return DomainResult.Error(
                IllegalArgumentException("End time must not be before start time"),
                MessageKey.EVENT_END_BEFORE_START,
            )
        }
        return repository.updateEvent(event)
    }
}
