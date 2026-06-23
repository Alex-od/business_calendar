package ua.danichapps.radiantdays.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ua.danichapps.radiantdays.BuildConfig
import ua.danichapps.radiantdays.R

@Composable
fun DebugAiLogsSideMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(stringResource(R.string.settings_show_ai_logs)) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
