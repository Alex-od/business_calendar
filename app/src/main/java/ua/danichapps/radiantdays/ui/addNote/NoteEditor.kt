package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import ua.danichapps.radiantdays.ui.common.NoteFormatToolbar
import ua.danichapps.radiantdays.ui.common.RichNoteTextField
import ua.danichapps.radiantdays.ui.common.appendVoiceTextToRichFieldValue
import ua.danichapps.radiantdays.ui.common.applyBoldTyping
import ua.danichapps.radiantdays.ui.common.noteFieldValueToMarkdown
import ua.danichapps.radiantdays.ui.common.noteMarkdownToFieldValue
import ua.danichapps.radiantdays.ui.common.preserveSpansOnEdit
import ua.danichapps.radiantdays.ui.common.rememberVoiceInputLauncher
import java.util.Locale

private val MinChatAreaHeight = 80.dp
private val ChatDividerVerticalSpace = 12.dp

@Stable
internal class NoteEditorState(
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
internal fun rememberNoteEditorState(
    description: String,
    editingNoteId: Long?,
    locale: Locale,
    noteDisplayStyles: NoteDisplayStyles,
    onDescriptionChange: (String) -> Unit,
    onDescriptionChangeFromVoice: (String) -> Unit,
    onDescriptionUndo: () -> Unit,
    onVoiceInputUnavailable: () -> Unit,
): NoteEditorState {
    var boldTyping by remember { mutableStateOf(false) }
    var isDescriptionFocused by remember { mutableStateOf(false) }
    var forceExternalSync by remember { mutableIntStateOf(0) }
    var localMarkdown by remember(editingNoteId) { mutableStateOf(description) }

    var descriptionValue by remember(editingNoteId) {
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

    return NoteEditorState(
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

/** Toolbar row shared by the note editor and the chat message edit dialog. */
@Composable
internal fun NoteEditorToolbarRow(
    state: NoteEditorState,
    uiState: AddEditNoteUiState,
    noteDisplayStyles: NoteDisplayStyles,
    canUndo: Boolean = uiState.canUndoDescription,
) {
    val showFormatToolbar = uiState.showFormatToolbar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = state.onDescriptionUndoClick,
            enabled = canUndo,
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
    }
}

/** Rich note field with toolbar, voice input, and optional chat list. */
@Composable
internal fun NoteEditor(
    state: NoteEditorState,
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
    noteDisplayStyles: NoteDisplayStyles,
    modifier: Modifier = Modifier,
    onMessageClick: (Int) -> Unit = {},
) {
    val bodyTextStyle = MaterialTheme.typography.bodyLarge
    val hasAiChatContent = uiState.aiChatMessages.isNotEmpty() || uiState.aiChatLoading
    val isAiChatMessagesVisible = uiState.showAiChat && hasAiChatContent
    val hasAiResponse = uiState.aiChatMessages.any { it.role == AiChatRole.ASSISTANT }
    val showNoteEditor = !isAiChatMessagesVisible || !hasAiResponse
    val oneLineHeight = with(LocalDensity.current) { bodyTextStyle.lineHeight.toDp() }

    Column(modifier = modifier) {
        NoteEditorToolbarRow(
            state = state,
            uiState = uiState,
            noteDisplayStyles = noteDisplayStyles,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (showNoteEditor) {
                if (isAiChatMessagesVisible) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val maxNoteHeight = (maxHeight - MinChatAreaHeight - ChatDividerVerticalSpace)
                            .coerceAtLeast(oneLineHeight)
                        val lineCount = state.descriptionValue.text.lineSequence().count().coerceAtLeast(1)
                        val estimatedNoteHeight = oneLineHeight * lineCount
                        val needsScroll = estimatedNoteHeight > maxNoteHeight
                        RichNoteTextField(
                            value = state.descriptionValue,
                            onFocusChange = state::onFocusChange,
                            onValueChange = state::onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = oneLineHeight,
                                    max = if (needsScroll) {
                                        maxNoteHeight
                                    } else {
                                        estimatedNoteHeight.coerceAtMost(maxNoteHeight)
                                    },
                                ),
                            textStyle = bodyTextStyle,
                            minLines = 1,
                            scrollEnabled = needsScroll,
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
            }

            if (isAiChatMessagesVisible) {
                if (showNoteEditor) {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (showNoteEditor) {
                                Modifier.heightIn(min = MinChatAreaHeight)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    InlineAiChatMessages(
                        messages = uiState.aiChatMessages,
                        noteDescription = uiState.description,
                        hasAiResponse = hasAiResponse,
                        loading = uiState.aiChatLoading,
                        onMessageClick = onMessageClick,
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
