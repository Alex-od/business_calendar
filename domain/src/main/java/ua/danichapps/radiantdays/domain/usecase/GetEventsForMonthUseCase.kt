package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

/**
 * Returns a reactive [Flow] of **all** [CalendarEvent] objects whose
 * [CalendarEvent.startTimeMillis] falls within [[monthStartMillis], [monthEndMillis]).
 *
 * Used by the calendar grid to show per-day event summaries without a separate
 * query for every visible day.
 *
 * @param repository Data source abstraction (injected).
 */
class GetEventsForMonthUseCase(private val repository: CalendarEventRepository) {

    /**
     * @param monthStartMillis  Inclusive start вЂ” midnight of the 1st of the month.
     * @param monthEndMillis    Exclusive end   вЂ” midnight of the 1st of the next month.
     */
    operator fun invoke(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CalendarEvent>> =
        repository.getEventsForDay(monthStartMillis, monthEndMillis)
}
