package ua.danichapps.radiantdays.domain.usecase

import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/** Loads notes that still need a one-time reminder work scheduled. */
class GetPendingRemindersUseCase(private val repository: CalendarEventRepository) {

    suspend operator fun invoke(fromMillis: Long): DomainResult<List<CalendarEvent>> =
        repository.getPendingReminders(fromMillis)
}
