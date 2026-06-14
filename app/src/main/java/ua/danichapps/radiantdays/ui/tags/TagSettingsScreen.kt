package ua.danichapps.radiantdays.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
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
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.ui.common.TagColorDot
import ua.danichapps.radiantdays.ui.common.dialog.TagEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    returnAfterCreate: Boolean = false,
    onTagCreated: (String) -> Unit = {},
    viewModel: TagSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TagSettingsUiEvent.TagCreated -> {
                    showAddDialog = false
                    if (returnAfterCreate) {
                        onTagCreated(event.tagGuid)
                        onNavigateBack()
                    }
                }
                is TagSettingsUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Теги") },
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
                    viewModel.clearAddTagError()
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Добавить тег")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TagSettingsContent(
            uiState = uiState,
            onEditTag = viewModel::requestEdit,
            onDeleteTag = viewModel::deleteTag,
            onTogglePinned = viewModel::toggleTagPinned,
            onOpenTag = onOpenTag,
            modifier = Modifier.padding(padding),
        )
    }

    if (showAddDialog) {
        TagEditDialog(
            title = "Добавить тег",
            initialName = "",
            initialColor = EventColor.DEFAULT,
            confirmText = "Добавить",
            externalError = uiState.tagNameError,
            onConfirm = viewModel::addTag,
            onInputChange = viewModel::clearAddTagError,
            onDismiss = {
                showAddDialog = false
                viewModel.clearAddTagError()
            },
        )
    }

    uiState.editingTag?.let { tag ->
        TagEditDialog(
            title = "Редактировать тег",
            initialName = tag.name,
            initialColor = tag.color,
            confirmText = "Сохранить",
            onConfirm = viewModel::updateTag,
            onDismiss = viewModel::dismissEdit,
        )
    }
}

@Composable
private fun TagSettingsContent(
    uiState: TagSettingsUiState,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onTogglePinned: (Tag) -> Unit,
    onOpenTag: (String) -> Unit,
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
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            items(
                items = uiState.tags,
                key = { tag -> tag.guid },
            ) { tag ->
                TagItem(
                    tag = tag,
                    onOpen = { onOpenTag(tag.guid) },
                    onTogglePinned = { onTogglePinned(tag) },
                    onEdit = { onEditTag(tag) },
                    onDelete = { onDeleteTag(tag) },
                )
            }
        }
    }
}

@Composable
private fun TagItem(
    tag: Tag,
    onOpen: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val pinColor = if (tag.isPinned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = if (tag.isUntaggedFilter) {
            null
        } else {
            { TagColorDot(color = tag.color) }
        },
        headlineContent = { Text(tag.name) },
        trailingContent = if (tag.isUntaggedFilter) {
            null
        } else {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onTogglePinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Закрепить",
                            tint = pinColor,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        },
    )
}
