package ua.danichapps.radiantdays.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
    SettingsListItem(
        headline = stringResource(R.string.settings_show_ai_logs),
        icon = Icons.Default.Description,
        onClick = onClick,
        modifier = modifier,
    )
}
