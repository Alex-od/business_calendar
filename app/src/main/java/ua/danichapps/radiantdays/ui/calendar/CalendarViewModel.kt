package ua.danichapps.radiantdays.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.domain.usecase.DeleteEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForDayUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForMonthUseCase
import java.util.Calendar

/**
 * ViewModel for the calendar screen.
 *
 * **State vs Events:**
 * - [uiState] вЂ” persistent UI state, collected with `collectAsStateWithLifecycle`.
 * - [events]  вЂ” one-shot events via [Channel]; consumed once and never re-triggered
 *   after configuration change.
 *
 * Two concurrent reactive subscriptions are maintained:
 * - [eventsJob]      вЂ” events for the **selected day** (bottom event list).
 * - [monthEventsJob] вЂ” all events for the **visible month** (grid cell summaries).
 */
class CalendarViewModel(
    private val getEventsForDayUseCase: GetEventsForDayUseCase,
    private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /** Buffered channel for one-time UI events (errors). Consumed in the screen. */
    private val _events = Channel<CalendarUiEvent>(Channel.BUFFERED)
    val events: Flow<CalendarUiEvent> = _events.receiveAsFlow()

    private var eventsJob: Job? = null
    private var monthEventsJob: Job? = null

    init {
        val nowMillis = System.currentTimeMillis()
        selectDay(nowMillis)
        loadMonthEvents(nowMillis)
    }

    // в”Ђв”Ђ User actions в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    /**
     * Selects [dayMillis] as the active day; subscribes a new reactive events flow for it.
     */
    fun selectDay(dayMillis: Long) {
        val (start, end) = dayWindow(dayMillis)
        _uiState.update { it.copy(selectedDayMillis = dayMillis, isLoading = true) }

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            getEventsForDayUseCase(start, end)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(CalendarUiEvent.ShowError(e.message ?: "Unknown error"))
                }
                .collect { events ->
                    _uiState.update { it.copy(eventsForDay = events, isLoading = false) }
                }
        }
    }

    fun navigateToPreviousMonth() = shiftMonth(-1)
    fun navigateToNextMonth()     = shiftMonth(+1)

    /** Deletes event [id]; emits [CalendarUiEvent.ShowError] on failure. */
    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            deleteEventUseCase(id)
                .onSuccess { alarmScheduler.cancel(id) }
                .onError { _, message ->
                    _events.send(CalendarUiEvent.ShowError(message))
                }
        }
    }

    // в”Ђв”Ђ Private helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    /**
     * Shifts the displayed month by [delta] and reloads the month-level event map.
     */
    private fun shiftMonth(delta: Int) {
        _uiState.update { state ->
            val cal = Calendar.getInstance().apply { timeInMillis = state.currentMonthMillis }
            cal.add(Calendar.MONTH, delta)
            state.copy(currentMonthMillis = cal.timeInMillis)
        }
        loadMonthEvents(_uiState.value.currentMonthMillis)
    }

    /**
     * Subscribes to all events in the visible month and groups them by their
     * normalised day-midnight millis so the grid can look them up in O(1).
     */
    private fun loadMonthEvents(monthMillis: Long) {
        val (start, end) = monthWindow(monthMillis)

        monthEventsJob?.cancel()
        monthEventsJob = viewModelScope.launch {
            getEventsForMonthUseCase(start, end)
                .catch { e ->
                    _events.send(CalendarUiEvent.ShowError(e.message ?: "Unknown error"))
                }
                .collect { events ->
                    val grouped = events.groupBy { event -> normaliseToDayStart(event.startTimeMillis) }
                    _uiState.update { it.copy(eventsForMonth = grouped) }
                }
        }
    }

    /** Midnight of the given epoch millis in the device's local timezone. */
    private fun normaliseToDayStart(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun dayWindow(millis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun monthWindow(monthMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
        val year  = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }
}
