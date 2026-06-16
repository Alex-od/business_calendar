package ua.danichapps.radiantdays.ui.common.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R

private data class PromptInsertToken(@StringRes val labelRes: Int, val token: String)

private val PROMPT_INSERT_TOKENS = listOf(
    PromptInsertToken(R.string.ai_action_token_text, "{{text}}"),
    PromptInsertToken(R.string.ai_action_token_title, "{{title}}"),
    PromptInsertToken(R.string.ai_action_token_tags, "{{tags}}"),
    PromptInsertToken(R.string.ai_action_token_date, "{{date}}"),
)

private fun insertAtCursor(current: TextFieldValue, insert: String): TextFieldValue {
    val pos = current.selection.start.coerceIn(0, current.text.length)
    val newText = current.text.substring(0, pos) + insert + current.text.substring(pos)
    return TextFieldValue(newText, TextRange(pos + insert.length))
}

@Composable
fun AiActionEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialPrompt: String,
    initialIsVisible: Boolean,
    confirmText: String,
    onConfirm: (name: String, description: String?, prompt: String, isVisible: Boolean) -> Unit,
    onDismiss: () -> Unit,
    externalError: String? = null,
    onInputChange: () -> Unit = {},
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var promptValue by remember { mutableStateOf(TextFieldValue(initialPrompt)) }
    var isVisible by remember { mutableStateOf(initialIsVisible) }
    var localError by remember { mutableStateOf<String?>(null) }
    val nameRequiredError = stringResource(R.string.ai_action_name_required_local)
    val promptRequiredError = stringResource(R.string.ai_action_prompt_required_local)

    LaunchedEffect(initialName, initialDescription, initialPrompt, initialIsVisible) {
        name = initialName
        description = initialDescription
        promptValue = TextFieldValue(initialPrompt)
        isVisible = initialIsVisible
        localError = null
    }

    LaunchedEffect(externalError) {
        if (externalError != null) localError = null
    }

    val displayedError = externalError ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        name.isBlank() -> localError = nameRequiredError
                        promptValue.text.isBlank() -> localError = promptRequiredError
                        else -> onConfirm(
                            name,
                            description.trim().takeIf { it.isNotBlank() },
                            promptValue.text,
                            isVisible,
                        )
                    }
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.ai_action_name)) },
                    isError = displayedError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.ai_action_description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = promptValue,
                    onValueChange = {
                        promptValue = it
                        localError = null
                        onInputChange()
                    },
                    label = { Text(stringResource(R.string.ai_action_prompt)) },
                    supportingText = {
                        Text(
                            stringResource(R.string.ai_action_prompt_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.ai_action_insert_from_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PROMPT_INSERT_TOKENS.forEach { item ->
                        AssistChip(
                            onClick = {
                                promptValue = insertAtCursor(promptValue, item.token)
                                localError = null
                                onInputChange()
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.ai_action_show_in_menu))
                Switch(
                    checked = isVisible,
                    onCheckedChange = { isVisible = it },
                )
                displayedError?.let { message ->
                    Spacer(Modifier.height(4.dp))
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}
