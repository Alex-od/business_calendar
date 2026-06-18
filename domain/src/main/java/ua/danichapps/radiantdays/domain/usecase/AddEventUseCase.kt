package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class AddEventUseCase(private val repository: CalendarEventRepository) {

    suspend operator fun invoke(event: CalendarEvent): DomainResult<Long> {
        if (event.title.isBlank() && event.description.isBlank() && event.aiChatMessages.isEmpty()) {
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
        return repository.addEvent(event)
    }
}
