package ua.danichapps.radiantdays.domain.usecase

import kotlinx.coroutines.flow.Flow
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository

class GetEventsByTagUseCase(
    private val repository: CalendarEventRepository,
) {
    operator fun invoke(tagGuid: String): Flow<List<CalendarEvent>> =
        if (Tag.isUntaggedFilter(tagGuid)) {
            repository.getEventsWithoutTags()
        } else {
            repository.getEventsByTagGuid(tagGuid)
        }
}
