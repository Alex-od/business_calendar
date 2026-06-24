package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.R

/** ViewModel actions bound once at the screen container; not passed as [AddEditNoteViewModel]. */
internal data class AddEditNoteViewModelActions(
    val events: Flow<AddEditNoteUiEvent>,
    val refreshAiKeyStatus: () -> Unit,
    val loadNote: (Long) -> Unit,
    val setInitialDay: (Long) -> Unit,
    val onTagAddedFromSettings: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onTagToggle: (String) -> Unit,
    val onTagsExpandedToggle: () -> Unit,
    val onAiChatSend: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onDescriptionChangeFromVoice: (String) -> Unit,
    val onDescriptionUndo: () -> Unit,
    val onAlarmTimeChange: (Long) -> Unit,
    val onNotificationMinutesChange: (Int) -> Unit,
    val onAddAlarmClick: () -> Unit,
    val onRemoveAlarmClick: () -> Unit,
    val onAiButtonClick: () -> Unit,
    val openAiActionsSheet: () -> Unit,
    val onAiChatMessageEdit: (Int, String) -> Unit,
    val onAiChatMessageDelete: (Int) -> Unit,
    val onShowFormatToolbarChange: (Boolean) -> Unit,
    val onShowAiChatChange: (Boolean) -> Unit,
    val onAiSheetDismiss: () -> Unit,
    val onAiActionSelected: (String) -> Unit,
)

@Composable
internal fun rememberAddEditNoteViewModelActions(
    viewModel: AddEditNoteViewModel,
): AddEditNoteViewModelActions = remember(viewModel) {
    AddEditNoteViewModelActions(
        events = viewModel.events,
        refreshAiKeyStatus = viewModel::refreshAiKeyStatus,
        loadNote = viewModel::loadNote,
        setInitialDay = viewModel::setInitialDay,
        onTagAddedFromSettings = viewModel::onTagAddedFromSettings,
        onBackClick = viewModel::onBackClick,
        onTagToggle = viewModel::onTagToggle,
        onTagsExpandedToggle = viewModel::onTagsExpandedToggle,
        onAiChatSend = viewModel::onAiChatSend,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDescriptionChangeFromVoice = viewModel::onDescriptionChangeFromVoice,
        onDescriptionUndo = viewModel::onDescriptionUndo,
        onAlarmTimeChange = viewModel::onAlarmTimeChange,
        onNotificationMinutesChange = viewModel::onNotificationMinutesChange,
        onAddAlarmClick = viewModel::onAddAlarmClick,
        onRemoveAlarmClick = viewModel::onRemoveAlarmClick,
        onAiButtonClick = viewModel::onAiButtonClick,
        openAiActionsSheet = viewModel::openAiActionsSheet,
        onAiChatMessageEdit = viewModel::onAiChatMessageEdit,
        onAiChatMessageDelete = viewModel::onAiChatMessageDelete,
        onShowFormatToolbarChange = viewModel::onShowFormatToolbarChange,
        onShowAiChatChange = viewModel::onShowAiChatChange,
        onAiSheetDismiss = viewModel::onAiSheetDismiss,
        onAiActionSelected = viewModel::onAiActionSelected,
    )
}

@Stable
internal data class AddEditNoteScreenCallbacks(
    val onBackClick: () -> Unit = {},
    val onTagToggle: (String) -> Unit = {},
    val onTagsExpandedToggle: () -> Unit = {},
    val onAiChatSend: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onDescriptionChangeFromVoice: (String) -> Unit = {},
    val onDescriptionUndo: () -> Unit = {},
    val onAlarmTimeChange: (Long) -> Unit = {},
    val onNotificationMinutesChange: (Int) -> Unit = {},
    val onAddAlarm: () -> Unit = {},
    val onRemoveAlarm: () -> Unit = {},
    val onAiClick: () -> Unit = {},
    val onAiActionsOpen: () -> Unit = {},
    val onAiChatMessageEdit: (Int, String) -> Unit = { _, _ -> },
    val onAiChatMessageDelete: (Int) -> Unit = {},
    val onAiChatMessageCopied: () -> Unit = {},
    val onVoiceInputUnavailable: () -> Unit = {},
    val onShowFormatToolbarChange: (Boolean) -> Unit = {},
    val onShowAiChatChange: (Boolean) -> Unit = {},
    val onAiSheetDismiss: () -> Unit = {},
    val onAiActionSelected: (String) -> Unit = {},
    val onOpenTags: () -> Unit = {},
    val onOpenAiActions: () -> Unit = {},
)

/** Binds screen actions and UI helpers into a stable callbacks holder. */
@Composable
internal fun rememberAddEditNoteScreenCallbacks(
    actions: AddEditNoteViewModelActions,
    snackbarHostState: SnackbarHostState,
    requestAlarmWithPermission: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit,
): AddEditNoteScreenCallbacks {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceUnavailableMessage = stringResource(R.string.event_voice_unavailable)

    return remember(
        actions,
        snackbarHostState,
        requestAlarmWithPermission,
        onOpenTags,
        onOpenAiActions,
        context,
        voiceUnavailableMessage,
    ) {
        AddEditNoteScreenCallbacks(
            onBackClick = actions.onBackClick,
            onTagToggle = actions.onTagToggle,
            onTagsExpandedToggle = actions.onTagsExpandedToggle,
            onAiChatSend = actions.onAiChatSend,
            onDescriptionChange = actions.onDescriptionChange,
            onDescriptionChangeFromVoice = actions.onDescriptionChangeFromVoice,
            onDescriptionUndo = actions.onDescriptionUndo,
            onAlarmTimeChange = actions.onAlarmTimeChange,
            onNotificationMinutesChange = actions.onNotificationMinutesChange,
            onAddAlarm = requestAlarmWithPermission,
            onRemoveAlarm = actions.onRemoveAlarmClick,
            onAiClick = actions.onAiButtonClick,
            onAiActionsOpen = actions.openAiActionsSheet,
            onAiChatMessageEdit = actions.onAiChatMessageEdit,
            onAiChatMessageDelete = actions.onAiChatMessageDelete,
            onAiChatMessageCopied = {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.chat_message_copied),
                    )
                }
            },
            onVoiceInputUnavailable = {
                scope.launch {
                    snackbarHostState.showSnackbar(voiceUnavailableMessage)
                }
            },
            onShowFormatToolbarChange = actions.onShowFormatToolbarChange,
            onShowAiChatChange = actions.onShowAiChatChange,
            onAiSheetDismiss = actions.onAiSheetDismiss,
            onAiActionSelected = actions.onAiActionSelected,
            onOpenTags = onOpenTags,
            onOpenAiActions = onOpenAiActions,
        )
    }
}
