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
import ua.danichapps.radiantdays.domain.model.MessageKey
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.domain.model.onError
import ua.danichapps.radiantdays.domain.model.onSuccess
import ua.danichapps.radiantdays.domain.usecase.AddTagUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteTagUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateTagUseCase
import ua.danichapps.radiantdays.locale.DomainErrorStrings

class TagSettingsViewModel(
    private val getTagsUseCase: GetTagsUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val errorStrings: DomainErrorStrings,
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
                .onError { _, key, args ->
                    if (isFieldLevelAddTagError(key)) {
                        _uiState.update { it.copy(tagNameError = errorStrings.resolve(key, args)) }
                    } else {
                        _events.send(TagSettingsUiEvent.ShowError(errorStrings.resolve(key, args)))
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
                .onError { _, key, args ->
                    _events.send(TagSettingsUiEvent.ShowError(errorStrings.resolve(key, args)))
                }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            deleteTagUseCase(tag.guid).onError { _, key, args ->
                _events.send(TagSettingsUiEvent.ShowError(errorStrings.resolve(key, args)))
            }
        }
    }

    fun toggleTagPinned(tag: Tag) {
        viewModelScope.launch {
            updateTagUseCase(tag.copy(isPinned = !tag.isPinned))
                .onError { _, key, args ->
                    _events.send(TagSettingsUiEvent.ShowError(errorStrings.resolve(key, args)))
                }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        TagSettingsUiEvent.ShowError(
                            errorStrings.resolve(MessageKey.LOAD_TAGS_FAILED),
                        ),
                    )
                }
                .collect { tags ->
                    val tagsWithUntagged = listOf(Tag.untaggedFilter()) + tags
                    _uiState.update { it.copy(isLoading = false, tags = tagsWithUntagged) }
                }
        }
    }

    private fun isFieldLevelAddTagError(key: MessageKey): Boolean =
        key == MessageKey.TAG_NAME_REQUIRED || key == MessageKey.TAG_NAME_TAKEN
}
