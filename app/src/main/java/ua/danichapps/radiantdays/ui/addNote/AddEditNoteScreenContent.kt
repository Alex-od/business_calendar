package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.domain.model.visibleContent
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
    locale: Locale,
) {
    var editingMessageIndex by remember { mutableIntStateOf(-1) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screenCallbacks = remember(callbacks, scope, drawerState) {
        callbacks.copy(
            onOpenSettings = {
                scope.launch { drawerState.open() }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TagToolbar(uiState = uiState, callbacks = screenCallbacks) },
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
                    drawerState = drawerState,
                    onMessageClick = { editingMessageIndex = it },
                )
            }
        }

        val editingMessage = uiState.aiChatMessages.getOrNull(editingMessageIndex)
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 8 },
            exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / 8 },
            modifier = Modifier.fillMaxSize(),
        ) {
            val message = editingMessage ?: return@AnimatedVisibility
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
                editingNoteId = uiState.editingNoteId,
                showFormatToolbar = uiState.showFormatToolbar,
                callbacks = callbacks,
                noteDisplayStyles = noteDisplayStyles,
                locale = locale,
                onDismiss = { editingMessageIndex = -1 },
            )
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
    drawerState: DrawerState,
    onMessageClick: (Int) -> Unit,
) {
    Box(
        KeyboardInsetsPolicy.editorContentModifier(
            base = Modifier.fillMaxSize().padding(padding),
            hasImeAwareBottomBar = uiState.showAiChat,
        ),
    ) {
        NoteForm(
            uiState = uiState,
            callbacks = callbacks,
            locale = locale,
            drawerState = drawerState,
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
                .padding(horizontal = AddEditNoteContentPadding),
        ),
    )
}
