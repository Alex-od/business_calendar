package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R

@Composable
fun NoteFormatToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    styles: NoteDisplayStyles,
    boldTyping: Boolean,
    onBoldTypingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatState = remember(value.text, value.selection, boldTyping) {
        noteFormatStateAt(value, styles, boldTyping)
    }
    val activeColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
    )
    val defaultColors = IconButtonDefaults.iconButtonColors()

    fun publish(newValue: TextFieldValue) {
        onValueChange(newValue)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = {
                publish(toggleBold(value, boldTyping, onBoldTypingChange))
            },
            colors = if (formatState.isBold) activeColors else defaultColors,
        ) {
            Icon(Icons.Default.FormatBold, contentDescription = stringResource(R.string.note_format_bold))
        }
        IconButton(
            onClick = { publish(toggleBulletLine(value)) },
            colors = if (formatState.isBullet) activeColors else defaultColors,
        ) {
            Icon(Icons.Default.FormatListBulleted, contentDescription = stringResource(R.string.note_format_list))
        }
        TextButton(
            onClick = { publish(setLineFontSize(value, NoteFontSize.Small, styles)) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (formatState.fontSize == NoteFontSize.Small) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text("A−", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = { publish(setLineFontSize(value, NoteFontSize.Normal, styles)) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (formatState.fontSize == NoteFontSize.Normal) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text("A", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = { publish(setLineFontSize(value, NoteFontSize.Large, styles)) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (formatState.fontSize == NoteFontSize.Large) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text("A+", style = MaterialTheme.typography.labelLarge)
        }
    }
}
