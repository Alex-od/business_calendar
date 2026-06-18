package ua.danichapps.radiantdays.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ai.AiModelOption
import ua.danichapps.radiantdays.ai.AiModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenAiActions: () -> Unit = {},
    viewModel: AiSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var errorDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AiSettingsUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is AiSettingsUiEvent.ShowErrorDialog -> errorDialog = event.title to event.message
            }
        }
    }

    errorDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { errorDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorDialog = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_ai)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                AiSettingsCard(
                    isKeySaved = uiState.isKeySaved,
                    isApiKeySectionExpanded = uiState.isApiKeySectionExpanded,
                    isValidatingApiKey = uiState.isValidatingApiKey,
                    apiKeyInput = uiState.apiKeyInput,
                    selectedModelId = uiState.selectedModelId,
                    availableModels = uiState.availableModels,
                    onOpenAiActions = onOpenAiActions,
                    onToggleApiKeySection = viewModel::onToggleApiKeySection,
                    onApiKeyChange = viewModel::onApiKeyChange,
                    onSaveApiKey = viewModel::saveApiKey,
                    onClearApiKey = viewModel::clearApiKey,
                    onModelSelected = viewModel::onModelSelected,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsCard(
    isKeySaved: Boolean,
    isApiKeySectionExpanded: Boolean,
    isValidatingApiKey: Boolean,
    apiKeyInput: String,
    selectedModelId: String,
    availableModels: List<AiModelOption>,
    onOpenAiActions: () -> Unit,
    onToggleApiKeySection: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            ListItem(
                modifier = Modifier.clickable(onClick = onToggleApiKeySection),
                headlineContent = { Text(stringResource(R.string.settings_openai_api_key)) },
                supportingContent = if (isKeySaved) {
                    { Text(stringResource(R.string.settings_ai_status_connected)) }
                } else {
                    {
                        Text(
                            text = stringResource(R.string.settings_api_not_added),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            if (isApiKeySectionExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = onApiKeyChange,
                        label = { Text(stringResource(R.string.settings_openai_api_key)) },
                        supportingText = {
                            Text(stringResource(R.string.settings_api_key_hint))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isValidatingApiKey,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSaveApiKey,
                            enabled = !isValidatingApiKey,
                        ) {
                            if (isValidatingApiKey) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                        TextButton(
                            onClick = onClearApiKey,
                            enabled = !isValidatingApiKey,
                        ) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            HorizontalDivider()
            AiModelSelector(
                selectedModelId = selectedModelId,
                availableModels = availableModels,
                onModelSelected = onModelSelected,
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAiActions),
                headlineContent = { Text(stringResource(R.string.settings_ai_actions)) },
                leadingContent = {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun AiModelSelector(
    selectedModelId: String,
    availableModels: List<AiModelOption>,
    onModelSelected: (String) -> Unit,
) {
    val selectedModel = AiModels.findById(selectedModelId) ?: availableModels.first()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            headlineContent = { Text(stringResource(R.string.settings_model)) },
            supportingContent = { Text(selectedModel.displayName) },
            leadingContent = {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.displayName) },
                    onClick = {
                        expanded = false
                        onModelSelected(model.id)
                    },
                )
            }
        }
    }
}
