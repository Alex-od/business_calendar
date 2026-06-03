package ua.danichapps.radiantdays.ui.addevent

import ua.danichapps.radiantdays.domain.model.Folder

/**
 * Immutable snapshot of the add/edit event form state.
 *
 * Contains only **persistent** state (survives config change).
 * One-shot events (navigation, errors) are emitted via [AddEditEventViewModel.events] channel.
 *
 * @property editingEventId Non-null в†’ edit mode. `null` в†’ add mode.
 * @property descriptionError Inline validation error shown beneath the note field.
 */
data class AddEditEventUiState(
    val isLoading: Boolean = false,
    val editingEventId: Long? = null,

    // Form fields
    val description: String = "",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val notificationMinutesBefore: Int = 0,
    val alarmTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val selectedFolderGuid: String? = null,

    // Inline field validation (stays in state вЂ” it IS persistent UI state)
    val descriptionError: String? = null,
)
