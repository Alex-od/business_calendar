package ua.danichapps.radiantdays.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.ui.common.ColoredTagChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterDialog(
    tags: List<Tag>,
    selectedGuids: Set<String>,
    onToggleTag: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_filter_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.calendar_filter_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        val name = if (tag.isUntaggedFilter) {
                            stringResource(R.string.tag_untagged)
                        } else {
                            tag.name
                        }
                        ColoredTagChip(
                            name = name,
                            color = tag.color,
                            selected = tag.guid in selectedGuids,
                            onClick = { onToggleTag(tag.guid) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.action_clear))
            }
        },
    )
}
