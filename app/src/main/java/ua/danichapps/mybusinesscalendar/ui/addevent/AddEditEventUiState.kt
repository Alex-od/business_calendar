package ua.danichapps.mybusinesscalendar.ui.addevent

/**
 * Immutable snapshot of the add/edit event form state.
 *
 * Contains only **persistent** state (survives config change).
 * One-shot events (navigation, errors) are emitted via [AddEditEventViewModel.events] channel.
 *
 * @property editingEventId Non-null → edit mode. `null` → add mode.
 * @property titleError     Inline validation error shown beneath the title field.
 */
data class AddEditEventUiState(
    val isLoading: Boolean = false,
    val editingEventId: Long? = null,

    // Form fields
    val title: String = "",
    val description: String = "",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long   = System.currentTimeMillis() + 60 * 60 * 1_000L,
    val isAllDay: Boolean = false,
    val notificationMinutesBefore: Int = 30,

    // Inline field validation (stays in state — it IS persistent UI state)
    val titleError: String? = null,
)
