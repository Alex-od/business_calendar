package ua.danichapps.radiantdays.ui.aiactions

sealed interface AiActionsUiEvent {
    data class ShowError(val message: String) : AiActionsUiEvent
    data object ActionSaved : AiActionsUiEvent
}
