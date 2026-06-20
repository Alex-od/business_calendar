package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole

private const val FIRST_MESSAGE_COLLAPSE_MAX_LINES = 4

/** Returns true when chat text exceeds the collapsed first-message limit. */
private fun isLongChatText(text: String): Boolean =
    text.lineSequence().count() > FIRST_MESSAGE_COLLAPSE_MAX_LINES || text.length > 200

internal data class ChatMessageBubbleState(
    val message: AiChatMessage,
    val displayContent: String,
    val isFirstMessage: Boolean,
    val isFirstMessageExpanded: Boolean,
    val isEditing: Boolean,
    val isMenuExpanded: Boolean,
    val loading: Boolean,
)

internal data class ChatMessageBubbleCallbacks(
    val onEnterEdit: () -> Unit,
    val onExpandOnly: () -> Unit,
    val onExpandAndEdit: () -> Unit,
    val onDismissMenu: () -> Unit,
    val onShowMenu: () -> Unit,
    val onContentChange: (String) -> Unit,
    val onFinishEdit: () -> Unit,
    val onCopy: () -> Unit,
    val onDelete: () -> Unit,
)

/** Single chat bubble with inline edit, collapse, and context menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageBubble(
    state: ChatMessageBubbleState,
    callbacks: ChatMessageBubbleCallbacks,
) {
    val message = state.message
    val displayContent = state.displayContent
    val isFirstMessage = state.isFirstMessage
    val isFirstMessageExpanded = state.isFirstMessageExpanded
    val isEditing = state.isEditing
    val isMenuExpanded = state.isMenuExpanded
    val loading = state.loading
    val isUser = message.role == AiChatRole.USER
    val canEdit = isUser
    val isEditingMessage = canEdit && isEditing
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val clipboardManager = LocalClipboardManager.current
    var draft by remember(displayContent) { mutableStateOf(displayContent) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember(isEditingMessage) { mutableStateOf(false) }
    val isLongText = isLongChatText(displayContent)
    val showCollapsed = isUser && isFirstMessage && isLongText && !isFirstMessageExpanded && !isEditingMessage
    val useFullWidth = !isUser || (isFirstMessage && (isFirstMessageExpanded || isEditingMessage))

    LaunchedEffect(displayContent, isEditingMessage) {
        if (!isEditingMessage) draft = displayContent
    }

    LaunchedEffect(isEditingMessage) {
        if (isEditingMessage) {
            hadFocus = false
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = if (useFullWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 300.dp)
            },
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            message.actionLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Box {
                val bubbleModifier = if (isEditingMessage) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        enabled = !loading,
                        onClick = when {
                            canEdit && isFirstMessage -> callbacks.onExpandAndEdit
                            canEdit -> callbacks.onEnterEdit
                            else -> ({})
                        },
                        onLongClick = callbacks.onShowMenu,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bubbleColor,
                    modifier = bubbleModifier,
                ) {
                    if (isEditingMessage) {
                        BasicTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                callbacks.onContentChange(it)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { callbacks.onFinishEdit() }),
                            minLines = if (isFirstMessage) 6 else 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        hadFocus = true
                                    } else if (hadFocus && isEditingMessage) {
                                        callbacks.onFinishEdit()
                                    }
                                },
                        )
                    } else {
                        ChatBubbleText(
                            text = displayContent,
                            textColor = textColor,
                            selectable = !canEdit,
                            showCollapsed = showCollapsed,
                        )
                    }
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = callbacks.onDismissMenu,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy)) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(displayContent))
                            callbacks.onCopy()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = callbacks.onDelete,
                    )
                }
            }
        }
    }
}

/** Read-only bubble text, optionally collapsed and selectable. */
@Composable
private fun ChatBubbleText(
    text: String,
    textColor: Color,
    selectable: Boolean,
    showCollapsed: Boolean,
) {
    val textModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    val maxLines = if (showCollapsed) FIRST_MESSAGE_COLLAPSE_MAX_LINES else Int.MAX_VALUE
    val overflow = if (showCollapsed) TextOverflow.Ellipsis else TextOverflow.Clip

    if (selectable) {
        SelectionContainer(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = maxLines,
                overflow = overflow,
                modifier = textModifier,
            )
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = maxLines,
            overflow = overflow,
            modifier = textModifier,
        )
    }
}
