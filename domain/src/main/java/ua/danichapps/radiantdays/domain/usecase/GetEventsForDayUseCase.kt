package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/**
 * Returns a reactive [Flow] of [CalendarEvent] objects for the given day window.
 *
 * The ViewModel receives live updates whenever data changes in the database,
 * without any polling or manual refresh logic.
 *
 * @param repository Data source abstraction (injected).
 */
class GetEventsForDayUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param dayStartMillis Inclusive start of the day (00:00:00 UTC) in epoch ms.
     * @param dayEndMillis   Exclusive end of the day (00:00:00 of next day) in epoch ms.
     */
    operator fun invoke(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEvent>> =
        repository.getEventsForDay(dayStartMillis, dayEndMillis)
}
