package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.visibleContent

private data class IndexedChatMessage(
    val index: Int,
    val message: AiChatMessage,
)

private const val CONTENT_TYPE_ACTION_LABEL = "action_label"
private const val CONTENT_TYPE_USER_MESSAGE = "user_message"
private const val CONTENT_TYPE_ASSISTANT_MESSAGE = "assistant_message"
private const val CONTENT_TYPE_LOADING = "loading"

/** Action label of the first user message when it mirrors the note text and is hidden. */
private fun List<AiChatMessage>.hiddenFirstActionLabel(
    noteDescription: String,
    hasAiResponse: Boolean,
): String? {
    if (hasAiResponse) return null
    val first = firstOrNull() ?: return null
    if (first.role != AiChatRole.USER || first.actionLabel == null) return null
    if (!first.shouldHideAsNoteMirror(noteDescription)) return null
    return first.actionLabel
}

/** Messages to render; after an AI reply the first user message is always shown as a bubble. */
private fun List<AiChatMessage>.visibleIndexedMessages(
    noteDescription: String,
    hasAiResponse: Boolean,
): List<IndexedChatMessage> =
    mapIndexed { index, message -> IndexedChatMessage(index, message) }
        .filter { (index, message) ->
            hasAiResponse ||
                !(index == 0 && message.role == AiChatRole.USER && message.shouldHideAsNoteMirror(noteDescription))
        }

/** True when the first user message duplicates the note and should not appear in chat. */
private fun AiChatMessage.shouldHideAsNoteMirror(noteDescription: String): Boolean {
    if (actionLabel != null) return true
    if (noteDescription.isBlank()) return false
    return visibleContent(noteDescription).trim() == noteDescription.trim()
}

/** Scrollable list of AI chat bubbles with edit, copy, and delete. */
@Composable
fun InlineAiChatMessages(
    messages: List<AiChatMessage>,
    noteDescription: String,
    hasAiResponse: Boolean,
    loading: Boolean,
    onMessageClick: (Int) -> Unit,
    onMessageDelete: (Int) -> Unit,
    onMessageCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val hiddenActionLabel = remember(messages, noteDescription, hasAiResponse) {
        messages.hiddenFirstActionLabel(noteDescription, hasAiResponse)
    }
    val visibleMessages = remember(messages, noteDescription, hasAiResponse) {
        messages.visibleIndexedMessages(noteDescription, hasAiResponse)
    }

    LaunchedEffect(messages.isEmpty()) {
        if (messages.isEmpty()) {
            menuIndex = -1
        }
    }

    val inPreview = LocalInspectionMode.current

    LaunchedEffect(messages.size, loading, hiddenActionLabel, listState.layoutInfo.totalItemsCount) {
        if (inPreview) return@LaunchedEffect
        if (visibleMessages.isEmpty() && !loading) return@LaunchedEffect
        val labelOffset = if (hiddenActionLabel != null) 1 else 0
        val lastItemIndex = labelOffset + visibleMessages.lastIndex + if (loading) 1 else 0
        if (listState.layoutInfo.totalItemsCount <= lastItemIndex) return@LaunchedEffect
        listState.animateScrollToItem(lastItemIndex)
    }

    if (messages.isEmpty() && !loading) return

    /** Closes the context menu on the active message. */
    fun dismissMenu() {
        menuIndex = -1
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 0.dp),
    ) {
        hiddenActionLabel?.let { label ->
            item(key = "action-label", contentType = CONTENT_TYPE_ACTION_LABEL) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        itemsIndexed(
            visibleMessages,
            key = { _, item -> "${item.index}-${item.message.role}" },
            contentType = { _, item ->
                if (item.message.role == AiChatRole.USER) {
                    CONTENT_TYPE_USER_MESSAGE
                } else {
                    CONTENT_TYPE_ASSISTANT_MESSAGE
                }
            },
        ) { _, item ->
            val displayContent = item.message.visibleContent(noteDescription)
            ChatMessageBubble(
                state = ChatMessageBubbleState(
                    message = item.message,
                    displayContent = displayContent,
                    isFirstMessage = item.index == 0 && item.message.role == AiChatRole.USER,
                    isMenuExpanded = menuIndex == item.index,
                    loading = loading,
                ),
                callbacks = ChatMessageBubbleCallbacks(
                    onOpenEditor = {
                        if (!loading) {
                            dismissMenu()
                            onMessageClick(item.index)
                        }
                    },
                    onDismissMenu = ::dismissMenu,
                    onShowMenu = {
                        if (!loading) menuIndex = item.index
                    },
                    onCopy = {
                        dismissMenu()
                        onMessageCopied()
                    },
                    onDelete = {
                        dismissMenu()
                        onMessageDelete(item.index)
                    },
                ),
            )
        }
        if (loading) {
            item(key = "loading", contentType = CONTENT_TYPE_LOADING) {
                AiChatLoadingIndicator()
            }
        }
    }
}

/** Spinner row shown while waiting for an assistant reply. */
@Composable
private fun AiChatLoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(8.dp),
            strokeWidth = 2.dp,
        )
    }
}
