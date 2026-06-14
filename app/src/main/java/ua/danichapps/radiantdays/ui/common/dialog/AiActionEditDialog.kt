package ua.danichapps.radiantdays.ui.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp

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
    var prompt by remember { mutableStateOf(initialPrompt) }
    var isVisible by remember { mutableStateOf(initialIsVisible) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialName, initialDescription, initialPrompt, initialIsVisible) {
        name = initialName
        description = initialDescription
        prompt = initialPrompt
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
                        name.isBlank() -> localError = "Введите название действия"
                        prompt.isBlank() -> localError = "Введите промпт"
                        else -> onConfirm(
                            name,
                            description.trim().takeIf { it.isNotBlank() },
                            prompt,
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
                Text("Отмена")
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
                    label = { Text("Название") },
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
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = {
                        prompt = it
                        localError = null
                        onInputChange()
                    },
                    label = { Text("Промпт") },
                    supportingText = {
                        Text(
                            "Плейсхолдеры: {{text}}, {{title}}, {{tags}}, {{date}}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Отображать в меню")
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
