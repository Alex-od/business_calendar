package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.R

internal data class AddEditEventScreenCallbacks(
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

/** Binds ViewModel and UI helpers into a stable callbacks holder. */
@Composable
internal fun rememberAddEditEventScreenCallbacks(
    viewModel: AddEditEventViewModel,
    snackbarHostState: SnackbarHostState,
    requestAlarmWithPermission: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit,
): AddEditEventScreenCallbacks {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceUnavailableMessage = stringResource(R.string.event_voice_unavailable)

    return remember(
        viewModel,
        snackbarHostState,
        requestAlarmWithPermission,
        onOpenTags,
        onOpenAiActions,
        context,
        voiceUnavailableMessage,
    ) {
        AddEditEventScreenCallbacks(
            onBackClick = viewModel::onBackClick,
            onTagToggle = viewModel::onTagToggle,
            onTagsExpandedToggle = viewModel::onTagsExpandedToggle,
            onAiChatSend = viewModel::onAiChatSend,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDescriptionChangeFromVoice = viewModel::onDescriptionChangeFromVoice,
            onDescriptionUndo = viewModel::onDescriptionUndo,
            onAlarmTimeChange = viewModel::onAlarmTimeChange,
            onNotificationMinutesChange = viewModel::onNotificationMinutesChange,
            onAddAlarm = requestAlarmWithPermission,
            onRemoveAlarm = viewModel::onRemoveAlarmClick,
            onAiClick = viewModel::onAiButtonClick,
            onAiActionsOpen = viewModel::openAiActionsSheet,
            onAiChatMessageEdit = viewModel::onAiChatMessageEdit,
            onAiChatMessageDelete = viewModel::onAiChatMessageDelete,
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
            onShowFormatToolbarChange = viewModel::onShowFormatToolbarChange,
            onShowAiChatChange = viewModel::onShowAiChatChange,
            onAiSheetDismiss = viewModel::onAiSheetDismiss,
            onAiActionSelected = viewModel::onAiActionSelected,
            onOpenTags = onOpenTags,
            onOpenAiActions = onOpenAiActions,
        )
    }
}
