package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Shared screen for adding a new event and editing an existing one.
 *
 * @param initialDayMillis Pre-sets start time when adding (ignored in edit mode).
 * @param editingNoteId   Non-null -> edit mode; `null` -> add mode.
 * @param onNavigateBack   Pops the back stack when the user leaves the screen.
 * @param viewModel        Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    initialDayMillis: Long,
    editingNoteId: Long?,
    onNavigateBack: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit = {},
    createdTagGuid: String? = null,
    onCreatedTagGuidConsumed: () -> Unit = {},
    viewModel: AddEditNoteViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    AddEditNoteScreenEffects(
        editingNoteId = editingNoteId,
        initialDayMillis = initialDayMillis,
        createdTagGuid = createdTagGuid,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onCreatedTagGuidConsumed = onCreatedTagGuidConsumed,
        snackbarHostState = snackbarHostState,
    )

    val requestAlarmWithPermission = rememberAlarmNotificationPermissionRequest(
        onGranted = viewModel::onAddAlarmClick,
    )
    val callbacks = rememberAddEditNoteScreenCallbacks(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        requestAlarmWithPermission = requestAlarmWithPermission,
        onOpenTags = onOpenTags,
        onOpenAiActions = onOpenAiActions,
    )

    AddEditNoteScreenContent(
        uiState = uiState,
        callbacks = callbacks,
        snackbarHostState = snackbarHostState,
    )
}
