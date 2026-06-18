package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.domain.model.visibleContent

@Composable
fun InlineAiChatSection(
    messages: List<AiChatMessage>,
    noteDescription: String,
    loading: Boolean,
    onSend: (String) -> Unit,
    onMessageEdit: (Int, String) -> Unit,
    onMessageDelete: (Int) -> Unit,
    onReplace: () -> Unit,
    onAppend: () -> Unit,
    onMessageCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var menuIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val lastAssistantIndex = messages.indexOfLast { it.role == AiChatRole.ASSISTANT }

    LaunchedEffect(messages.size, loading) {
        val lastIndex = messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(messages, key = { index, message -> "$index-${message.role}" }) { index, message ->
                ChatMessageBubble(
                    message = message,
                    displayContent = message.visibleContent(noteDescription),
                    isEditing = editingIndex == index,
                    isMenuExpanded = menuIndex == index,
                    showAssistantActions = index == lastAssistantIndex && message.role == AiChatRole.ASSISTANT,
                    loading = loading,
                    onEnterEdit = {
                        menuIndex = -1
                        editingIndex = index
                    },
                    onDismissMenu = { menuIndex = -1 },
                    onShowMenu = {
                        if (!loading) menuIndex = index
                    },
                    onContentChange = { onMessageEdit(index, it) },
                    onFinishEdit = { editingIndex = -1 },
                    onCopy = {
                        menuIndex = -1
                        onMessageCopied()
                    },
                    onDelete = {
                        menuIndex = -1
                        if (editingIndex == index) editingIndex = -1
                        onMessageDelete(index)
                    },
                    onReplace = onReplace,
                    onAppend = onAppend,
                )
            }
            if (loading) {
                item(key = "loading") {
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
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.ai_chat_message)) },
                    enabled = !loading,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val text = inputText
                        inputText = ""
                        onSend(text)
                    },
                    enabled = inputText.isNotBlank() && !loading,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: AiChatMessage,
    displayContent: String,
    isEditing: Boolean,
    isMenuExpanded: Boolean,
    showAssistantActions: Boolean,
    loading: Boolean,
    onEnterEdit: () -> Unit,
    onDismissMenu: () -> Unit,
    onShowMenu: () -> Unit,
    onContentChange: (String) -> Unit,
    onFinishEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    onAppend: () -> Unit,
) {
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
    var draft by remember(displayContent) { mutableStateOf(displayContent) }

    LaunchedEffect(displayContent, isEditing) {
        if (!isEditing) draft = displayContent
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
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
                val bubbleModifier = if (isEditing) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        enabled = !loading,
                        onClick = onEnterEdit,
                        onLongClick = onShowMenu,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bubbleColor,
                    modifier = bubbleModifier,
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                onContentChange(it)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onFinishEdit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .onFocusChanged { focus ->
                                    if (!focus.isFocused && isEditing) onFinishEdit()
                                },
                        )
                    } else {
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = onDismissMenu,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy)) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(displayContent))
                            onCopy()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = onDelete,
                    )
                }
            }

            if (showAssistantActions) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = onReplace,
                        enabled = !loading,
                    ) {
                        Text(
                            text = stringResource(R.string.action_replace),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(
                        onClick = onAppend,
                        enabled = !loading,
                    ) {
                        Text(
                            text = stringResource(R.string.action_add),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
