package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.Tag

fun List<Tag>.tagsForEvent(event: CalendarEvent): List<Tag> =
    filter { it.guid in event.tagGuids }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteTagsRow(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTags),
        verticalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTagRows),
    ) {
        tags.forEach { tag ->
            ReadOnlyTagChip(
                name = tag.name,
                color = tag.color,
            )
        }
    }
}
