package ua.danichapps.radiantdays.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.locale.AppLanguageOption
import ua.danichapps.radiantdays.locale.AppLanguages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLanguageSelector(
    selectedTag: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedOption = AppLanguages.findByTag(selectedTag) ?: AppLanguages.all.first()
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(ua.danichapps.radiantdays.R.string.settings_section_language)) },
        supportingContent = { Text(stringResource(ua.danichapps.radiantdays.R.string.settings_language_summary)) },
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    ) {
        OutlinedTextField(
            value = stringResource(selectedOption.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(ua.danichapps.radiantdays.R.string.settings_section_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguages.all.forEach { option ->
                LanguageMenuItem(
                    option = option,
                    onClick = {
                        expanded = false
                        onLanguageSelected(option.tag)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageMenuItem(
    option: AppLanguageOption,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(option.labelRes))
            }
        },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}
