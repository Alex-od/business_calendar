package ua.danichapps.radiantdays.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ui.theme.AppThemeMode
import ua.danichapps.radiantdays.ui.theme.AppThemeModes

@Composable
fun AppThemeSelector(
    selectedMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedOption = AppThemeModes.findByMode(selectedMode)

    SettingsDropdownSelector(
        headline = stringResource(R.string.settings_section_theme),
        supporting = stringResource(selectedOption.labelRes),
        icon = Icons.Default.DarkMode,
        modifier = modifier,
    ) { dismiss ->
        AppThemeModes.all.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.labelRes)) },
                onClick = {
                    dismiss()
                    onThemeModeSelected(option.mode)
                },
            )
        }
    }
}
