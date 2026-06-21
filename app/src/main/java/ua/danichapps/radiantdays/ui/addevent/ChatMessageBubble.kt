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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
    val isMenuExpanded: Boolean,
    val loading: Boolean,
)

internal data class ChatMessageBubbleCallbacks(
    val onOpenEditor: () -> Unit,
    val onDismissMenu: () -> Unit,
    val onShowMenu: () -> Unit,
    val onCopy: () -> Unit,
    val onDelete: () -> Unit,
)

/** Single chat bubble; tap opens the full-screen editor, long-press shows actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageBubble(
    state: ChatMessageBubbleState,
    callbacks: ChatMessageBubbleCallbacks,
) {
    val message = state.message
    val displayContent = state.displayContent
    val isFirstMessage = state.isFirstMessage
    val isMenuExpanded = state.isMenuExpanded
    val loading = state.loading
    val isUser = message.role == AiChatRole.USER
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
    val isLongText = isLongChatText(displayContent)
    val showCollapsed = isUser && isFirstMessage && isLongText
    val useFullWidth = !isUser || isFirstMessage

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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bubbleColor,
                    modifier = Modifier.combinedClickable(
                        enabled = !loading,
                        onClick = callbacks.onOpenEditor,
                        onLongClick = callbacks.onShowMenu,
                    ),
                ) {
                    ChatBubbleText(
                        text = displayContent,
                        textColor = textColor,
                        selectable = false,
                        showCollapsed = showCollapsed,
                    )
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

/** Read-only bubble text, optionally collapsed. */
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
