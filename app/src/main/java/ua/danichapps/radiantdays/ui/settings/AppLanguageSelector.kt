package ua.danichapps.radiantdays.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.locale.AppLanguages

@Composable
fun AppLanguageSelector(
    selectedTag: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedOption = AppLanguages.findByTag(selectedTag) ?: AppLanguages.all.first()

    SettingsDropdownSelector(
        headline = stringResource(R.string.settings_section_language),
        supporting = stringResource(selectedOption.labelRes),
        icon = Icons.Default.Language,
        modifier = modifier,
    ) { dismiss ->
        AppLanguages.all.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.labelRes)) },
                onClick = {
                    dismiss()
                    onLanguageSelected(option.tag)
                },
            )
        }
    }
}
