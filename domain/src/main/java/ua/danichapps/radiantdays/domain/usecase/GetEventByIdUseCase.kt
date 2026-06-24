package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class GetEventByIdUseCase(
    private val repository: CalendarEventRepository,
) {
    suspend operator fun invoke(id: Long): CalendarEvent? = repository.getEventById(id)
}
