package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.ui.common.KeyboardInsetsPolicy

/** Root scaffold: toolbar, form or loading state, AI chat bar, and actions sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditEventScreenContent(
    uiState: AddEditEventUiState,
    callbacks: AddEditEventScreenCallbacks,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        topBar = { TagToolbar(uiState = uiState, callbacks = callbacks) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AddEditEventAiChatBottomBar(uiState = uiState, callbacks = callbacks) },
    ) { padding ->
        when {
            uiState.isLoading -> AddEditEventLoadingContent(padding)
            else -> AddEditEventBody(uiState = uiState, callbacks = callbacks, padding = padding)
        }
    }

    AiActionsBottomSheet(
        visible = uiState.aiSheetVisible,
        actions = uiState.visibleAiActions,
        onDismiss = callbacks.onAiSheetDismiss,
        onActionSelected = callbacks.onAiActionSelected,
        onConfigureClick = {
            callbacks.onAiSheetDismiss()
            callbacks.onOpenAiActions()
        },
    )
}

/** Centered spinner shown while an existing event is loading. */
@Composable
private fun AddEditEventLoadingContent(padding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        CircularProgressIndicator()
    }
}

/** Main form area with optional AI-action loading overlay. */
@Composable
private fun AddEditEventBody(
    uiState: AddEditEventUiState,
    callbacks: AddEditEventScreenCallbacks,
    padding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().padding(padding)) {
        EventForm(
            uiState = uiState,
            callbacks = callbacks,
            modifier = Modifier.fillMaxSize(),
        )

        if (uiState.aiLoading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Bottom chat input bar; hidden when AI chat is disabled. */
@Composable
private fun AddEditEventAiChatBottomBar(
    uiState: AddEditEventUiState,
    callbacks: AddEditEventScreenCallbacks,
) {
    if (!uiState.showAiChat) return

    InlineAiChatInput(
        loading = uiState.aiChatLoading,
        onSend = callbacks.onAiChatSend,
        onAiActionsClick = callbacks.onAiActionsOpen,
        modifier = KeyboardInsetsPolicy.aiChatInputBarModifier(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ),
    )
}
