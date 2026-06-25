package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.CoroutineScope
import ua.danichapps.radiantdays.domain.model.Tag
import java.util.Calendar

private const val DEFAULT_ALARM_OFFSET_HOURS = 1L

/** Handles in-screen note editing interactions (description, undo, alarms, and tags). */
internal class NoteEditorFeature(
    scope: CoroutineScope,
    private val readState: () -> AddEditNoteUiState,
    private val updateState: ((AddEditNoteUiState) -> AddEditNoteUiState) -> Unit,
    private val onScheduleAutoSave: () -> Unit,
) {
    private val descriptionUndo = NoteDescriptionUndoController(scope)

    fun clearUndoHistory() {
        descriptionUndo.clear(::updateCanUndoDescription)
    }

    fun setInitialDay(dayMillis: Long) {
        clearUndoHistory()
        val startAt9 = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        updateState { it.copy(startTimeMillis = startAt9) }
    }

    fun onDescriptionChange(value: String) {
        val current = readState().description
        val next = descriptionUndo.onTypingChange(value, current, ::updateCanUndoDescription) ?: return
        setDescription(next)
    }

    fun onDescriptionChangeFromVoice(value: String) {
        applyDiscreteDescriptionChange(value)
    }

    fun applyDiscreteDescriptionChange(value: String) {
        val current = readState().description
        val next = descriptionUndo.onDiscreteChange(value, current, ::updateCanUndoDescription) ?: return
        setDescription(next)
    }

    fun onDescriptionUndo() {
        val previous = descriptionUndo.undo(::updateCanUndoDescription) ?: return
        setDescription(previous, scheduleSave = false)
    }

    fun onAddAlarmClick() {
        updateState { state ->
            if (state.alarmTimeMillis != null) return@updateState state
            state.copy(alarmTimeMillis = System.currentTimeMillis() + DEFAULT_ALARM_OFFSET_HOURS * 3_600_000L)
        }
        onScheduleAutoSave()
    }

    fun onRemoveAlarmClick() {
        updateState { it.copy(alarmTimeMillis = null) }
        onScheduleAutoSave()
    }

    fun onAlarmTimeChange(millis: Long) {
        updateState { it.copy(alarmTimeMillis = millis) }
        onScheduleAutoSave()
    }

    fun onIsCompletedChange(value: Boolean) {
        updateState { it.copy(isCompleted = value) }
        onScheduleAutoSave()
    }

    fun onNotificationMinutesChange(min: Int) {
        updateState { it.copy(notificationMinutesBefore = min) }
        onScheduleAutoSave()
    }

    fun onTagToggle(tagGuid: String) {
        updateState { state ->
            val next = if (tagGuid in state.selectedTagGuids) {
                state.selectedTagGuids - tagGuid
            } else {
                state.selectedTagGuids + tagGuid
            }
            state.copy(selectedTagGuids = next)
        }
        onScheduleAutoSave()
    }

    fun onTagAddedFromSettings(tagGuid: String) {
        updateState { state ->
            state.copy(selectedTagGuids = state.selectedTagGuids + tagGuid)
        }
        onScheduleAutoSave()
    }

    fun onTagsExpandedToggle() {
        updateState { it.copy(tagsExpanded = !it.tagsExpanded) }
    }

    fun applyAvailableTags(tags: List<Tag>) {
        updateState { state ->
            val validGuids = state.selectedTagGuids.filter { guid ->
                tags.any { tag -> tag.guid == guid }
            }.toSet()
            state.copy(tags = tags, selectedTagGuids = validGuids)
        }
    }

    private fun setDescription(value: String, scheduleSave: Boolean = true) {
        updateState { it.copy(description = value, descriptionError = null) }
        if (scheduleSave) {
            onScheduleAutoSave()
        }
    }

    private fun updateCanUndoDescription(canUndo: Boolean) {
        if (readState().canUndoDescription != canUndo) {
            updateState { it.copy(canUndoDescription = canUndo) }
        }
    }
}
