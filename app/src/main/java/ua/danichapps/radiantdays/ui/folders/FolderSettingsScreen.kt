package ua.danichapps.radiantdays.ui.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.domain.model.Folder
import ua.danichapps.radiantdays.ui.common.dialog.FolderNameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FolderSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FolderSettingsUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройка папок") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        FolderSettingsContent(
            uiState = uiState,
            onFolderNameChange = viewModel::onFolderNameChange,
            onAddFolder = viewModel::addFolder,
            onEditFolder = viewModel::requestEdit,
            onDeleteFolder = viewModel::deleteFolder,
            modifier = Modifier.padding(padding),
        )
    }

    uiState.editingFolder?.let { folder ->
        FolderNameDialog(
            title = "Редактировать папку",
            initialName = folder.name,
            confirmText = "Сохранить",
            onConfirm = viewModel::updateFolder,
            onDismiss = viewModel::dismissEdit,
        )
    }
}

@Composable
private fun FolderSettingsContent(
    uiState: FolderSettingsUiState,
    onFolderNameChange: (String) -> Unit,
    onAddFolder: () -> Unit,
    onEditFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.folderName,
            onValueChange = onFolderNameChange,
            label = { Text("Введите имя папки") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.folderNameError != null,
            supportingText = uiState.folderNameError?.let { message ->
                { Text(message, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Добавить")
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = uiState.folders,
                    key = { folder -> folder.guid },
                ) { folder ->
                    FolderItem(
                        folder = folder,
                        onEdit = { onEditFolder(folder) },
                        onDelete = { onDeleteFolder(folder) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: Folder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(folder.name) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        },
    )
}
