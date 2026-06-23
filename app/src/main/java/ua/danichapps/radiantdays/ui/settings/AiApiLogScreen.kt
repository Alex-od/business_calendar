package ua.danichapps.radiantdays.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ai.AiApiRequestLogSink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiApiLogScreen(
    onDismiss: () -> Unit,
    logSink: AiApiRequestLogSink = koinInject(),
) {
    var logText by remember { mutableStateOf(logSink.get().orEmpty()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.settings_ai_logs_copied)

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_ai_logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (logText.isBlank()) return@IconButton
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ai_api_log", logText))
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        },
                    ) {
                        Text(stringResource(R.string.settings_ai_logs_copy))
                    }
                    IconButton(
                        onClick = {
                            logSink.clear()
                            logText = ""
                        },
                    ) {
                        Text(stringResource(R.string.settings_ai_logs_clear))
                    }
                },
            )
        },
    ) { padding ->
        val displayText = logText.ifBlank { stringResource(R.string.settings_ai_logs_empty) }
        val textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            color = if (logText.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        SelectionContainer {
            Text(
                text = displayText,
                style = textStyle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
