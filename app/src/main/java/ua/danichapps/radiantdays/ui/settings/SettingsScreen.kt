package ua.danichapps.radiantdays.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import android.app.Activity
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
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
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenAiActions: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SettingsUiEvent.LocaleChanged -> activity?.recreate()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                AppLanguageSelector(
                    selectedTag = uiState.selectedLanguageTag,
                    onLanguageSelected = viewModel::onLanguageSelected,
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.settings_section_ai),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                AiSettingsCard(
                    aiStatus = uiState.aiStatus,
                    isKeySaved = uiState.isKeySaved,
                    isApiKeySectionExpanded = uiState.isApiKeySectionExpanded,
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
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.settings_section_organization),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                SettingsNavigationItem(
                    headline = stringResource(R.string.settings_tags),
                    supporting = stringResource(R.string.settings_tags_summary),
                    icon = Icons.AutoMirrored.Filled.Label,
                    onClick = onOpenTags,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsCard(
    aiStatus: AiConnectionStatus,
    isKeySaved: Boolean,
    isApiKeySectionExpanded: Boolean,
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
    val statusMessage = when (aiStatus) {
        AiConnectionStatus.CONNECTED -> stringResource(R.string.settings_ai_status_connected)
        AiConnectionStatus.STUB -> stringResource(R.string.settings_ai_status_stub)
    }

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
                supportingContent = { Text(statusMessage) },
                leadingContent = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (isKeySaved) {
                                        stringResource(R.string.settings_api_connected)
                                    } else {
                                        stringResource(R.string.settings_api_stub)
                                    },
                                )
                            },
                            enabled = false,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveApiKey) {
                            Text(stringResource(R.string.action_save))
                        }
                        TextButton(onClick = onClearApiKey) {
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
                supportingContent = { Text(stringResource(R.string.settings_ai_actions_summary)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiModelSelector(
    selectedModelId: String,
    availableModels: List<AiModelOption>,
    onModelSelected: (String) -> Unit,
) {
    val selectedModel = AiModels.findById(selectedModelId) ?: availableModels.first()
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_model)) },
        supportingContent = { Text(stringResource(selectedModel.descriptionRes)) },
        leadingContent = {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    ) {
        OutlinedTextField(
            value = selectedModel.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_selected_model)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableModels.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.displayName)
                            Text(
                                text = stringResource(model.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onModelSelected(model.id)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(
                imageVector = icon,
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
