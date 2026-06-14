package ua.danichapps.radiantdays.ui.tags

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
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddTagUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteTagUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateTagUseCase

class TagSettingsViewModel(
    private val getTagsUseCase: GetTagsUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagSettingsUiState())
    val uiState: StateFlow<TagSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<TagSettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<TagSettingsUiEvent> = _events.receiveAsFlow()

    init {
        observeTags()
    }

    fun clearAddTagError() {
        _uiState.update { it.copy(tagNameError = null) }
    }

    fun addTag(name: String, color: EventColor) {
        viewModelScope.launch {
            addTagUseCase(name, color)
                .onSuccess { tag ->
                    _uiState.update { it.copy(tagNameError = null) }
                    _events.send(TagSettingsUiEvent.TagCreated(tag.guid))
                }
                .onError { _, message ->
                    if (isFieldLevelAddTagError(message)) {
                        _uiState.update { it.copy(tagNameError = message) }
                    } else {
                        _events.send(TagSettingsUiEvent.ShowError(message))
                    }
                }
        }
    }

    fun requestEdit(tag: Tag) {
        _uiState.update { it.copy(editingTag = tag) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editingTag = null) }
    }

    fun updateTag(name: String, color: EventColor) {
        val tag = _uiState.value.editingTag ?: return
        viewModelScope.launch {
            updateTagUseCase(tag.copy(name = name, color = color))
                .onSuccess {
                    _uiState.update { it.copy(editingTag = null) }
                }
                .onError { _, message ->
                    _events.send(TagSettingsUiEvent.ShowError(message))
                }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            deleteTagUseCase(tag.guid).onError { _, message ->
                _events.send(TagSettingsUiEvent.ShowError(message))
            }
        }
    }

    fun toggleTagPinned(tag: Tag) {
        viewModelScope.launch {
            updateTagUseCase(tag.copy(isPinned = !tag.isPinned))
                .onError { _, message ->
                    _events.send(TagSettingsUiEvent.ShowError(message))
                }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        TagSettingsUiEvent.ShowError(throwable.message ?: "Не удалось загрузить теги"),
                    )
                }
                .collect { tags ->
                    val tagsWithUntagged = listOf(Tag.untaggedFilter()) + tags
                    _uiState.update { it.copy(isLoading = false, tags = tagsWithUntagged) }
                }
        }
    }

    private fun isFieldLevelAddTagError(message: String): Boolean =
        message == "Введите имя тега" || message == "Тег с таким именем уже существует"
}
