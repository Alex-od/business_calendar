package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.visibleContent

private data class IndexedChatMessage(
    val index: Int,
    val message: AiChatMessage,
)

private fun List<AiChatMessage>.hiddenFirstActionLabel(): String? =
    firstOrNull()
        ?.takeIf { it.role == AiChatRole.USER && it.actionLabel != null }
        ?.actionLabel

private fun List<AiChatMessage>.visibleIndexedMessages(): List<IndexedChatMessage> =
    mapIndexed { index, message -> IndexedChatMessage(index, message) }
        .filter { (index, message) ->
            !(index == 0 && message.role == AiChatRole.USER && message.actionLabel != null)
        }

@Composable
fun InlineAiChatMessages(
    messages: List<AiChatMessage>,
    noteDescription: String,
    loading: Boolean,
    onMessageEdit: (Int, String) -> Unit,
    onMessageDelete: (Int) -> Unit,
    onMessageCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingIndex by remember { mutableIntStateOf(-1) }
    var menuIndex by remember { mutableIntStateOf(-1) }
    var firstMessageExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val firstMessageResetKey = messages.firstOrNull()?.let { Triple(it.apiContent, it.actionLabel, it.role) }
    val hiddenActionLabel = remember(messages) { messages.hiddenFirstActionLabel() }
    val visibleMessages = remember(messages) { messages.visibleIndexedMessages() }

    LaunchedEffect(messages.isEmpty()) {
        if (messages.isEmpty()) {
            firstMessageExpanded = false
            editingIndex = -1
        }
    }

    LaunchedEffect(firstMessageResetKey) {
        firstMessageExpanded = false
        if (editingIndex == 0) editingIndex = -1
    }

    LaunchedEffect(messages.size, loading, hiddenActionLabel) {
        if (visibleMessages.isEmpty() && !loading) return@LaunchedEffect
        val labelOffset = if (hiddenActionLabel != null) 1 else 0
        val lastItemIndex = labelOffset + visibleMessages.lastIndex + if (loading) 1 else 0
        listState.animateScrollToItem(lastItemIndex)
    }

    if (messages.isEmpty() && !loading) return

    fun dismissMenu() {
        menuIndex = -1
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        hiddenActionLabel?.let { label ->
            item(key = "action-label") {
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
        ) { visibleIndex, item ->
            val displayContent = item.message.visibleContent(noteDescription)
            ChatMessageBubble(
                state = ChatMessageBubbleState(
                    message = item.message,
                    displayContent = displayContent,
                    isFirstMessage = visibleIndex == 0,
                    isFirstMessageExpanded = firstMessageExpanded,
                    isEditing = editingIndex == item.index,
                    isMenuExpanded = menuIndex == item.index,
                    loading = loading,
                ),
                callbacks = ChatMessageBubbleCallbacks(
                    onEnterEdit = {
                        if (item.message.role == AiChatRole.USER) {
                            dismissMenu()
                            editingIndex = item.index
                        }
                    },
                    onExpandOnly = {
                        dismissMenu()
                        firstMessageExpanded = true
                    },
                    onExpandAndEdit = {
                        dismissMenu()
                        firstMessageExpanded = true
                        editingIndex = item.index
                    },
                    onDismissMenu = ::dismissMenu,
                    onShowMenu = {
                        if (!loading) menuIndex = item.index
                    },
                    onContentChange = { onMessageEdit(item.index, it) },
                    onFinishEdit = { editingIndex = -1 },
                    onCopy = {
                        dismissMenu()
                        onMessageCopied()
                    },
                    onDelete = {
                        dismissMenu()
                        if (editingIndex == item.index) editingIndex = -1
                        onMessageDelete(item.index)
                    },
                ),
            )
        }
        if (loading) {
            item(key = "loading") {
                AiChatLoadingIndicator()
            }
        }
    }
}

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
