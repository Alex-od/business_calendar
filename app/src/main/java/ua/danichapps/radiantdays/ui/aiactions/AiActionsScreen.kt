package ua.danichapps.radiantdays.ui.aiactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.ui.aiactions.AiActionsViewModel
import ua.danichapps.radiantdays.ui.common.dialog.AiActionEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiActionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiActionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AiActionsUiEvent.ActionSaved -> showAddDialog = false
                is AiActionsUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI-действия") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.clearAddError()
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Text("Добавить действие")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AiActionsContent(
            uiState = uiState,
            onEdit = viewModel::requestEdit,
            onDelete = viewModel::deleteAction,
            onToggleVisible = viewModel::toggleVisible,
            onReorder = viewModel::reorderActions,
            modifier = Modifier.padding(padding),
        )
    }

    if (showAddDialog) {
        AiActionEditDialog(
            title = "Добавить действие",
            initialName = "",
            initialDescription = "",
            initialPrompt = "",
            initialIsVisible = true,
            confirmText = "Добавить",
            externalError = uiState.actionNameError,
            onConfirm = viewModel::addAction,
            onInputChange = viewModel::clearAddError,
            onDismiss = {
                showAddDialog = false
                viewModel.clearAddError()
            },
        )
    }

    uiState.editingAction?.let { action ->
        AiActionEditDialog(
            title = "Редактировать действие",
            initialName = action.name,
            initialDescription = action.description.orEmpty(),
            initialPrompt = action.prompt,
            initialIsVisible = action.isVisible,
            confirmText = "Сохранить",
            onConfirm = viewModel::updateAction,
            onDismiss = viewModel::dismissEdit,
        )
    }
}

@Composable
private fun AiActionsContent(
    uiState: AiActionsUiState,
    onEdit: (AiAction) -> Unit,
    onDelete: (AiAction) -> Unit,
    onToggleVisible: (AiAction) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        items(
            items = uiState.actions,
            key = { action -> action.guid },
        ) { action ->
            ReorderableItem(reorderableState, key = action.guid) { _ ->
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        IconButton(
                            modifier = Modifier.draggableHandle(),
                            onClick = {},
                        ) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Перетащить")
                        }
                    },
                    headlineContent = { Text(action.name) },
                    supportingContent = action.description?.let { description ->
                        { Text(description, style = MaterialTheme.typography.bodySmall) }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = action.isVisible,
                                onCheckedChange = { onToggleVisible(action) },
                            )
                            IconButton(onClick = { onEdit(action) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                            }
                            IconButton(
                                onClick = { onDelete(action) },
                                enabled = !action.isBuiltIn,
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    },
                )
            }
        }
    }
}
