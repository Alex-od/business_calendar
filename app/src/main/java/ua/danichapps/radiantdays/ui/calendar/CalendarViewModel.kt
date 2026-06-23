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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.calendar.dayWindow
import ua.danichapps.radiantdays.calendar.monthWindow
import ua.danichapps.radiantdays.calendar.normaliseToDayStart
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.matchesTagFilter
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.DeleteEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForDayUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForMonthUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.locale.DomainErrorStrings
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater
import java.util.Calendar

class CalendarViewModel(
    private val getEventsForDayUseCase: GetEventsForDayUseCase,
    private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
    private val errorStrings: DomainErrorStrings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _events = Channel<CalendarUiEvent>(Channel.BUFFERED)
    val events: Flow<CalendarUiEvent> = _events.receiveAsFlow()

    private val _selectedFilterTagGuids = MutableStateFlow<Set<String>>(emptySet())

    private var eventsJob: Job? = null
    private var monthEventsJob: Job? = null

    init {
        val nowMillis = System.currentTimeMillis()
        selectDay(nowMillis)
        loadMonthEvents(nowMillis)
        observeFilterTags()
    }

    fun toggleFilterTag(guid: String) {
        _selectedFilterTagGuids.update { current ->
            when {
                guid in current -> current - guid
                Tag.isUntaggedFilter(guid) -> setOf(guid)
                current.any { Tag.isUntaggedFilter(it) } -> (current - Tag.UNTAGGED_GUID) + guid
                else -> current + guid
            }
        }
        syncFilterToUiState()
    }

    fun clearTagFilter() {
        _selectedFilterTagGuids.value = emptySet()
        syncFilterToUiState()
    }

    fun selectDay(dayMillis: Long) {
        val (start, end) = dayWindow(dayMillis)
        _uiState.update { it.copy(selectedDayMillis = dayMillis, isLoading = true) }

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            combine(
                getEventsForDayUseCase(start, end),
                _selectedFilterTagGuids,
            ) { events, filter ->
                events.filter { it.matchesTagFilter(filter) }
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        CalendarUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.UNKNOWN, listOf(e.message.orEmpty())),
                        ),
                    )
                }
                .collect { filtered ->
                    _uiState.update { it.copy(eventsForDay = filtered, isLoading = false) }
                }
        }
    }

    fun navigateToPreviousMonth() = shiftMonth(-1)
    fun navigateToNextMonth() = shiftMonth(+1)

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            deleteEventUseCase(id)
                .onSuccess {
                    alarmScheduler.cancel(id)
                    widgetUpdater.refresh()
                }
                .onError { _, key, args ->
                    _events.send(CalendarUiEvent.ShowError(errorStrings.resolve(key, args)))
                }
        }
    }

    private fun shiftMonth(delta: Int) {
        _uiState.update { state ->
            val cal = Calendar.getInstance().apply { timeInMillis = state.currentMonthMillis }
            cal.add(Calendar.MONTH, delta)
            state.copy(currentMonthMillis = cal.timeInMillis)
        }
        loadMonthEvents(_uiState.value.currentMonthMillis)
    }

    private fun loadMonthEvents(monthMillis: Long) {
        val (start, end) = monthWindow(monthMillis)

        monthEventsJob?.cancel()
        monthEventsJob = viewModelScope.launch {
            combine(
                getEventsForMonthUseCase(start, end),
                _selectedFilterTagGuids,
            ) { events, filter ->
                events
                    .filter { it.matchesTagFilter(filter) }
                    .groupBy { event -> normaliseToDayStart(event.startTimeMillis) }
            }
                .catch { e ->
                    _events.send(
                        CalendarUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.UNKNOWN, listOf(e.message.orEmpty())),
                        ),
                    )
                }
                .collect { grouped ->
                    _uiState.update { it.copy(eventsForMonth = grouped) }
                }
        }
    }

    private fun observeFilterTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch {
                    _events.send(
                        CalendarUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.LOAD_TAGS_FAILED),
                        ),
                    )
                }
                .collect { tags ->
                    val dialogTags = listOf(Tag.untaggedFilter()) + tags
                    val validGuids = dialogTags.map { it.guid }.toSet()
                    _selectedFilterTagGuids.update { it.intersect(validGuids) }
                    syncFilterToUiState(dialogTags)
                }
        }
    }

    private fun syncFilterToUiState(dialogTags: List<Tag> = _uiState.value.filterDialogTags) {
        _uiState.update {
            it.copy(
                selectedFilterTagGuids = _selectedFilterTagGuids.value,
                filterDialogTags = dialogTags,
            )
        }
    }
}
