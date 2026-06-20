package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import ua.danichapps.radiantdays.ui.common.NoteFormatToolbar
import ua.danichapps.radiantdays.ui.common.RichNoteTextField
import ua.danichapps.radiantdays.ui.common.appendVoiceTextToRichFieldValue
import ua.danichapps.radiantdays.ui.common.applyBoldTyping
import ua.danichapps.radiantdays.ui.common.noteFieldValueToMarkdown
import ua.danichapps.radiantdays.ui.common.noteMarkdownToFieldValue
import ua.danichapps.radiantdays.ui.common.preserveSpansOnEdit
import ua.danichapps.radiantdays.ui.common.rememberVoiceInputLauncher
import org.koin.compose.koinInject

@Stable
internal class EventNoteEditorState(
    val descriptionValue: TextFieldValue,
    val boldTyping: Boolean,
    val isDescriptionFocused: Boolean,
    val startVoiceInput: () -> Unit,
    val onDescriptionUndoClick: () -> Unit,
    private val setDescriptionValue: (TextFieldValue) -> Unit,
    private val setBoldTyping: (Boolean) -> Unit,
    private val setIsDescriptionFocused: (Boolean) -> Unit,
) {
    /** Updates the rich-text field value. */
    fun updateDescriptionValue(value: TextFieldValue) = setDescriptionValue(value)

    /** Toggles bold typing mode for new characters. */
    fun updateBoldTyping(value: Boolean) = setBoldTyping(value)

    /** Tracks whether the description field has focus. */
    fun onFocusChange(focused: Boolean) = setIsDescriptionFocused(focused)

    /** Applies span preservation and optional bold typing on edit. */
    fun onValueChange(newValue: TextFieldValue) {
        val preserved = preserveSpansOnEdit(descriptionValue, newValue)
        val processed = if (boldTyping) {
            applyBoldTyping(descriptionValue, preserved)
        } else {
            preserved
        }
        setDescriptionValue(processed)
    }
}

/** Creates and remembers note editor state synced with ViewModel description. */
@Composable
internal fun rememberEventNoteEditorState(
    description: String,
    editingEventId: Long?,
    noteDisplayStyles: NoteDisplayStyles,
    onDescriptionChange: (String) -> Unit,
    onDescriptionChangeFromVoice: (String) -> Unit,
    onDescriptionUndo: () -> Unit,
    onVoiceInputUnavailable: () -> Unit,
): EventNoteEditorState {
    val context = LocalContext.current
    val localeStore: AppLocaleStore = koinInject()
    val locale = remember(context) { localeStore.resolveLocale(context) }

    var boldTyping by remember { mutableStateOf(false) }
    var isDescriptionFocused by remember { mutableStateOf(false) }
    var forceExternalSync by remember { mutableIntStateOf(0) }
    var localMarkdown by remember(editingEventId) { mutableStateOf(description) }

    var descriptionValue by remember(editingEventId) {
        mutableStateOf(
            noteMarkdownToFieldValue(
                markdown = description,
                styles = noteDisplayStyles,
            ),
        )
    }

    LaunchedEffect(descriptionValue) {
        delay(400)
        val markdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
        if (markdown == localMarkdown) return@LaunchedEffect
        localMarkdown = markdown
        onDescriptionChange(markdown)
    }

    LaunchedEffect(description, forceExternalSync) {
        if (isDescriptionFocused && forceExternalSync == 0) return@LaunchedEffect
        if (description == localMarkdown) {
            forceExternalSync = 0
            return@LaunchedEffect
        }
        val fieldMarkdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
        if (description == fieldMarkdown) {
            localMarkdown = description
            forceExternalSync = 0
            return@LaunchedEffect
        }
        localMarkdown = description
        descriptionValue = noteMarkdownToFieldValue(
            markdown = description,
            styles = noteDisplayStyles,
            selection = descriptionValue.selection,
        )
        boldTyping = false
        forceExternalSync = 0
    }

    val voicePrompt = stringResource(R.string.event_voice_prompt)
    val startVoiceInput = rememberVoiceInputLauncher(
        locale = locale,
        prompt = voicePrompt,
        onResult = { spoken ->
            descriptionValue = appendVoiceTextToRichFieldValue(descriptionValue, spoken)
            val markdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
            localMarkdown = markdown
            onDescriptionChangeFromVoice(markdown)
        },
        onUnavailable = onVoiceInputUnavailable,
    )

    return EventNoteEditorState(
        descriptionValue = descriptionValue,
        boldTyping = boldTyping,
        isDescriptionFocused = isDescriptionFocused,
        startVoiceInput = startVoiceInput,
        onDescriptionUndoClick = {
            onDescriptionUndo()
            forceExternalSync++
        },
        setDescriptionValue = { descriptionValue = it },
        setBoldTyping = { boldTyping = it },
        setIsDescriptionFocused = { isDescriptionFocused = it },
    )
}

/** Rich note field with toolbar, voice input, AI button, and optional chat list. */
@Composable
internal fun EventNoteEditor(
    state: EventNoteEditorState,
    uiState: AddEditEventUiState,
    callbacks: AddEditEventScreenCallbacks,
    noteDisplayStyles: NoteDisplayStyles,
    modifier: Modifier = Modifier,
) {
    val bodyTextStyle = MaterialTheme.typography.bodyLarge
    val showFormatToolbar = uiState.showFormatToolbar
    val hasAiChatContent = uiState.aiChatMessages.isNotEmpty() || uiState.aiChatLoading
    val isAiChatMessagesVisible = uiState.showAiChat && hasAiChatContent
    val noteMaxHeightWhenChatVisible = with(LocalDensity.current) {
        (bodyTextStyle.lineHeight.toPx() * 3).toDp()
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = state.onDescriptionUndoClick,
                enabled = uiState.canUndoDescription,
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            if (showFormatToolbar) {
                NoteFormatToolbar(
                    value = state.descriptionValue,
                    onValueChange = state::updateDescriptionValue,
                    styles = noteDisplayStyles,
                    boldTyping = state.boldTyping,
                    onBoldTypingChange = state::updateBoldTyping,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = state.startVoiceInput) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.event_voice_input))
            }
            IconButton(
                onClick = callbacks.onAiClick,
                enabled = uiState.isAiKeySaved && state.descriptionValue.text.isNotBlank(),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ai_chat_assistant))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (isAiChatMessagesVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(noteMaxHeightWhenChatVisible),
                ) {
                    RichNoteTextField(
                        value = state.descriptionValue,
                        onFocusChange = state::onFocusChange,
                        onValueChange = state::onValueChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = bodyTextStyle,
                        minLines = 1,
                    )
                }
            } else {
                RichNoteTextField(
                    value = state.descriptionValue,
                    onFocusChange = state::onFocusChange,
                    onValueChange = state::onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = bodyTextStyle,
                    minLines = 1,
                )
            }

            if (isAiChatMessagesVisible) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    InlineAiChatMessages(
                        messages = uiState.aiChatMessages,
                        noteDescription = uiState.description,
                        loading = uiState.aiChatLoading,
                        onMessageEdit = callbacks.onAiChatMessageEdit,
                        onMessageDelete = callbacks.onAiChatMessageDelete,
                        onMessageCopied = callbacks.onAiChatMessageCopied,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (uiState.descriptionError != null) {
                Text(
                    text = uiState.descriptionError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
