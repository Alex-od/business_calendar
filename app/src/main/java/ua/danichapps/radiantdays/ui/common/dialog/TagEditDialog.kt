package ua.danichapps.radiantdays.ui.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.ui.common.TagColorPicker

@Composable
fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: EventColor,
    confirmText: String,
    onConfirm: (name: String, color: EventColor) -> Unit,
    onDismiss: () -> Unit,
    externalError: String? = null,
    onInputChange: () -> Unit = {},
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialName, initialColor) {
        name = initialName
        color = initialColor
        localError = null
    }

    LaunchedEffect(externalError) {
        if (externalError != null) {
            localError = null
        }
    }

    val displayedError = externalError ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Введите имя тега"
                    } else {
                        onConfirm(name, color)
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
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                        onInputChange()
                    },
                    label = { Text("Введите имя тега") },
                    isError = displayedError != null,
                    supportingText = displayedError?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier,
                )
                Spacer(Modifier.height(12.dp))
                Text("Цвет", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                TagColorPicker(
                    selectedColor = color,
                    onColorSelected = {
                        color = it
                        onInputChange()
                    },
                )
            }
        },
    )
}
