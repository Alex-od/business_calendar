package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

/** Text field, AI actions button, and send button for continuing the AI chat. */
@Composable
fun InlineAiChatInput(
    loading: Boolean,
    onSend: (String) -> Unit,
    onAiActionsClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AiChatInputTestTags.BAR),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onAiActionsClick,
                modifier = Modifier.testTag(AiChatInputTestTags.ACTIONS),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.event_ai_actions))
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag(AiChatInputTestTags.FIELD)
                    .onFocusChanged { onFocusChange(it.isFocused) },
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

object AiChatInputTestTags {
    const val BAR = "ai_chat_input_bar"
    const val ACTIONS = "ai_chat_input_actions"
    const val FIELD = "ai_chat_input_field"
}

/** Preview: idle chat input. */
@Preview(showBackground = true, name = "Idle")
@Composable
private fun InlineAiChatInputPreview() {
    RadiantDaysTheme(dynamicColor = false) {
        InlineAiChatInput(
            loading = false,
            onSend = {},
            onAiActionsClick = {},
        )
    }
}

/** Preview: chat input while a message is sending. */
@Preview(showBackground = true, name = "Loading")
@Composable
private fun InlineAiChatInputLoadingPreview() {
    RadiantDaysTheme(dynamicColor = false) {
        InlineAiChatInput(
            loading = true,
            onSend = {},
            onAiActionsClick = {},
        )
    }
}
