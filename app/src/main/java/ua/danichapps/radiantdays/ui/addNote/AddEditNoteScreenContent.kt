package ua.danichapps.radiantdays.ui.addNote

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ua.danichapps.radiantdays.domain.model.visibleContent
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.ui.common.KeyboardInsetsPolicy
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import java.util.Locale

/** Root scaffold: toolbar, form or loading state, AI chat bar, and actions sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditNoteScreenContent(
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val localeStore: AppLocaleStore = koinInject()
    val locale = remember(context) { localeStore.resolveLocale(context) }
    var editingMessageIndex by remember { mutableIntStateOf(-1) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TagToolbar(uiState = uiState, callbacks = callbacks) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { AddEditNoteAiChatBottomBar(uiState = uiState, callbacks = callbacks) },
        ) { padding ->
            when {
                uiState.isLoading -> AddEditNoteLoadingContent(padding)
                else -> AddEditNoteBody(
                    uiState = uiState,
                    callbacks = callbacks,
                    locale = locale,
                    padding = padding,
                    onMessageClick = { editingMessageIndex = it },
                )
            }
        }

        if (editingMessageIndex >= 0) {
            val message = uiState.aiChatMessages.getOrNull(editingMessageIndex)
            if (message != null) {
                val typography = MaterialTheme.typography
                val noteDisplayStyles = remember(typography) {
                    NoteDisplayStyles(
                        smallSize = typography.labelSmall.fontSize,
                        normalSize = typography.bodyLarge.fontSize,
                        largeSize = typography.headlineSmall.fontSize,
                    )
                }
                ChatMessageEditScreen(
                    messageIndex = editingMessageIndex,
                    initialMarkdown = message.visibleContent(uiState.description),
                    messageRole = message.role,
                    uiState = uiState,
                    callbacks = callbacks,
                    noteDisplayStyles = noteDisplayStyles,
                    locale = locale,
                    onDismiss = { editingMessageIndex = -1 },
                )
            }
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
private fun AddEditNoteLoadingContent(padding: PaddingValues) {
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
private fun AddEditNoteBody(
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
    locale: Locale,
    padding: PaddingValues,
    onMessageClick: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(padding)) {
        NoteForm(
            uiState = uiState,
            callbacks = callbacks,
            locale = locale,
            onMessageClick = onMessageClick,
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
private fun AddEditNoteAiChatBottomBar(
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
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
