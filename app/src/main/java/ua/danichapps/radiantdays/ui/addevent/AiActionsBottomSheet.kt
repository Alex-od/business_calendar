package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiAction

@Composable
internal fun AiActionsBottomSheetContent(
    actions: List<AiAction>,
    onActionClick: (String) -> Unit,
    onConfigureClick: () -> Unit,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.event_ai_actions),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (actions.isEmpty()) {
            Text(
                text = stringResource(R.string.event_no_actions),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onConfigureClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.action_configure))
            }
        } else {
            LazyColumn {
                items(actions, key = { it.guid }) { action ->
                    ListItem(
                        modifier = Modifier.clickable { onActionClick(action.guid) },
                        headlineContent = { Text(action.name) },
                        supportingContent = action.description?.let { description ->
                            {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
