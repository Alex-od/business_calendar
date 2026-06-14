package ua.danichapps.radiantdays.ui.tagnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.DeleteEventUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsByTagUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater

class TagNotesViewModel(
    private val tagGuid: String,
    private val getEventsByTagUseCase: GetEventsByTagUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: CalendarWidgetUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TagNotesUiState(
            tagName = if (Tag.isUntaggedFilter(tagGuid)) Tag.UNTAGGED_NAME else "",
        ),
    )
    val uiState: StateFlow<TagNotesUiState> = _uiState.asStateFlow()

    private val _events = Channel<TagNotesUiEvent>(Channel.BUFFERED)
    val events: Flow<TagNotesUiEvent> = _events.receiveAsFlow()

    init {
        if (!Tag.isUntaggedFilter(tagGuid)) {
            resolveTagName()
        }
        observeNotes()
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            deleteEventUseCase(noteId)
                .onSuccess {
                    alarmScheduler.cancel(noteId)
                    widgetUpdater.refresh()
                }
                .onError { _, message ->
                    _events.send(TagNotesUiEvent.ShowError(message))
                }
        }
    }

    private fun resolveTagName() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch { }
                .collect { tags ->
                    val name = tags.find { it.guid == tagGuid }?.name
                    if (name != null) {
                        _uiState.update { it.copy(tagName = name) }
                    }
                }
        }
    }

    private fun observeNotes() {
        viewModelScope.launch {
            getEventsByTagUseCase(tagGuid)
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        TagNotesUiEvent.ShowError(
                            throwable.message ?: "Не удалось загрузить заметки",
                        ),
                    )
                }
                .collect { notes ->
                    _uiState.update { it.copy(isLoading = false, notes = notes) }
                }
        }
    }
}
