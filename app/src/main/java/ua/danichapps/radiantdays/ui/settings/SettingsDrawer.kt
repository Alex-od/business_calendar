package ua.danichapps.radiantdays.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.BuildConfig
import ua.danichapps.radiantdays.R

private val SettingsDrawerWidth = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    onOpenAiSettings: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenTagFilter: () -> Unit = {},
    tagFilterActiveCount: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    content: @Composable (openSettingsDrawer: () -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SettingsUiEvent.LocaleChanged -> activity?.recreate()
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshAiSummary()
        onPauseOrDispose { }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val openSettingsDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    var showAiLogScreen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(SettingsDrawerWidth),
            ) {
                SettingsPanelContent(
                    uiState = uiState,
                    onLanguageSelected = viewModel::onLanguageSelected,
                    onOpenAiSettings = {
                        scope.launch {
                            drawerState.close()
                            onOpenAiSettings()
                        }
                    },
                    onOpenTags = {
                        scope.launch {
                            drawerState.close()
                            onOpenTags()
                        }
                    },
                    onOpenTagFilter = {
                        scope.launch {
                            drawerState.close()
                            onOpenTagFilter()
                        }
                    },
                    tagFilterActiveCount = tagFilterActiveCount,
                    onShowAiLogs = {
                        scope.launch {
                            drawerState.close()
                            showAiLogScreen = true
                        }
                    },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            content(openSettingsDrawer)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            if (showAiLogScreen) {
                AiApiLogScreen(onDismiss = { showAiLogScreen = false })
            }
        }
    }
}

@Composable
internal fun SettingsPanelContent(
    uiState: SettingsUiState,
    onLanguageSelected: (String?) -> Unit,
    onOpenAiSettings: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenTagFilter: () -> Unit = {},
    tagFilterActiveCount: Int = 0,
    onShowAiLogs: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            AppLanguageSelector(
                selectedTag = uiState.selectedLanguageTag,
                onLanguageSelected = onLanguageSelected,
            )
        }
        item {
            SettingsNavigationItem(
                headline = stringResource(R.string.settings_section_ai),
                supporting = uiState.aiModelDisplayName,
                icon = Icons.Default.AutoAwesome,
                onClick = onOpenAiSettings,
            )
        }
        item {
            SettingsNavigationItem(
                headline = stringResource(R.string.settings_tags),
                icon = Icons.AutoMirrored.Filled.Label,
                onClick = onOpenTags,
            )
        }
        item {
            SettingsNavigationItem(
                headline = stringResource(R.string.settings_tag_filter),
                supporting = if (tagFilterActiveCount > 0) {
                    stringResource(R.string.calendar_filter_active_count, tagFilterActiveCount)
                } else {
                    null
                },
                icon = Icons.Default.FilterList,
                onClick = onOpenTagFilter,
            )
        }
        if (BuildConfig.DEBUG) {
            item {
                SettingsNavigationItem(
                    headline = stringResource(R.string.settings_show_ai_logs),
                    icon = Icons.Default.Description,
                    onClick = onShowAiLogs,
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    headline: String,
    icon: ImageVector,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { name ->
            { Text(name) }
        },
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
