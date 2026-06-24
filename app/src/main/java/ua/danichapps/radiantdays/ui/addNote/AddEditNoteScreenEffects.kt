package ua.danichapps.radiantdays.ui.addNote

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.koin.compose.koinInject
import ua.danichapps.radiantdays.locale.DomainErrorStrings
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Side effects: load event, collect one-shot events, handle back and new tags. */
@Composable
internal fun AddEditNoteScreenEffects(
    editingNoteId: Long?,
    initialDayMillis: Long,
    createdTagGuid: String?,
    actions: AddEditNoteViewModelActions,
    onNavigateBack: () -> Unit,
    onCreatedTagGuidConsumed: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val errorStrings: DomainErrorStrings = koinInject()

    LifecycleResumeEffect(Unit) {
        actions.refreshAiKeyStatus()
        onPauseOrDispose { }
    }

    LaunchedEffect(editingNoteId) {
        if (editingNoteId != null) actions.loadNote(editingNoteId)
        else actions.setInitialDay(initialDayMillis)
    }

    LaunchedEffect(Unit) {
        actions.events.collect { event ->
            when (event) {
                is AddEditNoteUiEvent.NavigateBack -> onNavigateBack()
                is AddEditNoteUiEvent.ShowError -> snackbarHostState.showSnackbar(
                    errorStrings.resolve(event.key, event.args, event.cause),
                )
                is AddEditNoteUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(createdTagGuid) {
        val tagGuid = createdTagGuid ?: return@LaunchedEffect
        actions.onTagAddedFromSettings(tagGuid)
        onCreatedTagGuidConsumed()
    }

    BackHandler(onBack = actions.onBackClick)
}

/** Returns a callback that requests POST_NOTIFICATIONS on API 33+ before adding an alarm. */
@Composable
internal fun rememberAlarmNotificationPermissionRequest(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> onGranted() }

    return remember(onGranted, context, permissionLauncher) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onGranted()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (granted) {
                    onGranted()
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
