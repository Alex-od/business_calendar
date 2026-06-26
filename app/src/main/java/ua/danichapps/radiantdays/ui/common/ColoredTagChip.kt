package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.domain.model.EventColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = TagChipSpacing.ChipContentPadding,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedLabelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    label: @Composable () -> Unit,
) {
    val backgroundColor = if (selected) selectedContainerColor else containerColor
    val contentColor = if (selected) selectedLabelColor else labelColor
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = FilterChipDefaults.shape,
        color = backgroundColor,
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected),
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                label()
            }
        }
    }
}

@Composable
fun ColoredTagChip(
    name: String,
    color: EventColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = color.toComposeColor()
    CompactFilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        selectedContainerColor = accent.copy(alpha = 0.28f),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TagColorDot(color = color)
                Spacer(Modifier.width(TagChipSpacing.DotToLabelSpacing))
                Text(name)
            }
        },
    )
}
