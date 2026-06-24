package ua.danichapps.radiantdays.ui.addNote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import ua.danichapps.radiantdays.ui.common.RichNoteTextField

private val ContentHorizontalPadding = 8.dp

/** Full-screen editor for a single chat message (title: You / AI). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatMessageEditScreen(
    messageIndex: Int,
    initialMarkdown: String,
    messageRole: AiChatRole,
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
    noteDisplayStyles: NoteDisplayStyles,
    locale: Locale,
    onDismiss: () -> Unit,
) {
    val editorState = rememberNoteEditorState(
        description = initialMarkdown,
        editingNoteId = uiState.editingNoteId,
        locale = locale,
        noteDisplayStyles = noteDisplayStyles,
        onDescriptionChange = { content ->
            callbacks.onAiChatMessageEdit(messageIndex, content)
        },
        onDescriptionChangeFromVoice = { content ->
            callbacks.onAiChatMessageEdit(messageIndex, content)
        },
        onDescriptionUndo = {},
        onVoiceInputUnavailable = callbacks.onVoiceInputUnavailable,
    )
    val bodyTextStyle = MaterialTheme.typography.bodyLarge
    val title = when (messageRole) {
        AiChatRole.USER -> stringResource(R.string.ai_chat_you)
        AiChatRole.ASSISTANT -> stringResource(R.string.ai_chat_assistant)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BackHandler(onBack = onDismiss)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = ContentHorizontalPadding),
            ) {
                NoteEditorToolbarRow(
                    state = editorState,
                    uiState = uiState,
                    noteDisplayStyles = noteDisplayStyles,
                    canUndo = false,
                )
                RichNoteTextField(
                    value = editorState.descriptionValue,
                    onFocusChange = editorState::onFocusChange,
                    onValueChange = editorState::onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = ContentHorizontalPadding),
                    textStyle = bodyTextStyle,
                    minLines = 1,
                )
            }
        }
    }
}
