package ua.danichapps.radiantdays.ui.aiactions

import ua.danichapps.radiantdays.domain.model.AiAction

data class AiActionsUiState(
    val isLoading: Boolean = true,
    val actions: List<AiAction> = emptyList(),
    val editingAction: AiAction? = null,
    val actionNameError: String? = null,
)
