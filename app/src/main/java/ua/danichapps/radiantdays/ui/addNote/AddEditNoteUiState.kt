package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.runtime.Immutable
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.Tag

@Immutable
data class AddEditNoteUiState(
    val isLoading: Boolean = false,
    val editingNoteId: Long? = null,

    val title: String = "",
    val description: String = "",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val notificationMinutesBefore: Int = 0,
    val alarmTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val selectedTagGuids: Set<String> = emptySet(),
    val tagsExpanded: Boolean = false,

    val descriptionError: String? = null,

    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,

    val visibleAiActions: List<AiAction> = emptyList(),
    val aiSheetVisible: Boolean = false,
    val aiLoading: Boolean = false,
    val aiChatMessages: List<AiChatMessage> = emptyList(),
    val aiChatLoading: Boolean = false,

    val canUndoDescription: Boolean = false,

    val isAiKeySaved: Boolean = false,

    val showFormatToolbar: Boolean = true,
    val showAiChat: Boolean = true,

    val saveStatus: NoteSaveStatus = NoteSaveStatus.Idle,
)
