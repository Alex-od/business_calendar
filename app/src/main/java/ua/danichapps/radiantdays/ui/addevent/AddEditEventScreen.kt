package ua.danichapps.radiantdays.ui.addevent

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ui.common.KeyboardInsetsPolicy

/**
 * Shared screen for adding a new event and editing an existing one.
 *
 * @param initialDayMillis Pre-sets start time when adding (ignored in edit mode).
 * @param editingEventId   Non-null -> edit mode; `null` -> add mode.
 * @param onNavigateBack   Pops the back stack when the user leaves the screen.
 * @param viewModel        Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    initialDayMillis: Long,
    editingEventId: Long?,
    onNavigateBack: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit = {},
    createdTagGuid: String? = null,
    onCreatedTagGuidConsumed: () -> Unit = {},
    viewModel: AddEditEventViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshAiKeyStatus()
        onPauseOrDispose { }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editingEventId) {
        if (editingEventId != null) viewModel.loadEvent(editingEventId)
        else viewModel.setInitialDay(initialDayMillis)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEventUiEvent.NavigateBack -> onNavigateBack()
                is AddEditEventUiEvent.ShowError    -> snackbarHostState.showSnackbar(event.message)
                is AddEditEventUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(createdTagGuid) {
        val tagGuid = createdTagGuid ?: return@LaunchedEffect
        viewModel.onTagAddedFromSettings(tagGuid)
        onCreatedTagGuidConsumed()
    }

    BackHandler(onBack = viewModel::onBackClick)

    val context = LocalContext.current
    val voiceUnavailableMessage = stringResource(R.string.event_voice_unavailable)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> viewModel.onAddAlarmClick() }

    fun requestAlarmWithPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onAddAlarmClick()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onAddAlarmClick()
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AddEditEventScreenContent(
        uiState = uiState,
        callbacks = AddEditEventScreenCallbacks(
            onBackClick = viewModel::onBackClick,
            onTagToggle = viewModel::onTagToggle,
            onTagsExpandedToggle = viewModel::onTagsExpandedToggle,
            onAiChatSend = viewModel::onAiChatSend,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDescriptionChangeFromVoice = viewModel::onDescriptionChangeFromVoice,
            onDescriptionUndo = viewModel::onDescriptionUndo,
            onAlarmTimeChange = viewModel::onAlarmTimeChange,
            onNotificationMinutesChange = viewModel::onNotificationMinutesChange,
            onAddAlarm = { requestAlarmWithPermission() },
            onRemoveAlarm = viewModel::onRemoveAlarmClick,
            onAiClick = viewModel::onAiButtonClick,
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
        ),
        snackbarHostState = snackbarHostState,
        onOpenTags = onOpenTags,
        onOpenAiActions = onOpenAiActions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditEventScreenContent(
    uiState: AddEditEventUiState,
    callbacks: AddEditEventScreenCallbacks,
    snackbarHostState: SnackbarHostState,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit,
) {
    Scaffold(
        topBar = {
            TagToolbar(
                uiState = uiState,
                onBackClick = callbacks.onBackClick,
                onTagToggle = callbacks.onTagToggle,
                onTagsExpandedToggle = callbacks.onTagsExpandedToggle,
                onOpenTags = onOpenTags,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.showAiChat) {
                InlineAiChatInput(
                    loading = uiState.aiChatLoading,
                    onSend = callbacks.onAiChatSend,
                    modifier = KeyboardInsetsPolicy.aiChatInputBarModifier(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ),
                )
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(64.dp))
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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

    if (uiState.aiSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = callbacks.onAiSheetDismiss,
            sheetState = sheetState,
        ) {
            AiActionsBottomSheetContent(
                actions = uiState.visibleAiActions,
                onActionClick = callbacks.onAiActionSelected,
                onConfigureClick = {
                    callbacks.onAiSheetDismiss()
                    onOpenAiActions()
                },
            )
        }
    }
}
