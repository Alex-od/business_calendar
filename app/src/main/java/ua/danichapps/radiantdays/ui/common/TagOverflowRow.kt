package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.Tag

@Composable
fun TagOverflowRow(
    tags: List<Tag>,
    tagsExpanded: Boolean,
    onTagToggle: (String) -> Unit,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tagsExpanded) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTags),
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            items(
                items = tags,
                key = { tag -> tag.guid },
            ) { tag ->
                TagQuickAccessChip(
                    tag = tag,
                    selected = false,
                    onClick = { onTagToggle(tag.guid) },
                )
            }
            item(key = "tag_actions") {
                TagQuickAccessActions(
                    showMore = true,
                    tagsExpanded = true,
                    onTagsExpandedToggle = onTagsExpandedToggle,
                    onOpenTags = onOpenTags,
                )
            }
        }
        return
    }

    SubcomposeLayout(
        modifier = modifier.fillMaxWidth(),
    ) { constraints ->
        val maxWidth = constraints.maxWidth
        val tagSpacingPx = TagChipSpacing.BetweenTags.roundToPx()
        val actionSpacingPx = TagChipSpacing.BetweenActionButtons.roundToPx()
        val looseConstraints = Constraints(minWidth = 0, maxWidth = Constraints.Infinity)

        val tagPlaceables = tags.map { tag ->
            subcompose("tag_${tag.guid}") {
                TagQuickAccessChip(
                    tag = tag,
                    selected = false,
                    onClick = { onTagToggle(tag.guid) },
                )
            }.first().measure(looseConstraints)
        }

        val addPlaceable = subcompose("add") {
            TagAddButton(onClick = onOpenTags)
        }.first().measure(looseConstraints)

        val morePlaceable = subcompose("more") {
            TagMoreButton(
                selected = false,
                onClick = onTagsExpandedToggle,
            )
        }.first().measure(looseConstraints)

        val addWidth = addPlaceable.width
        val moreWidth = morePlaceable.width

        var visibleCount = 0
        var usedWidth = 0

        for (index in tagPlaceables.indices) {
            val spacingBefore = if (visibleCount > 0) tagSpacingPx else 0
            val tagWidth = tagPlaceables[index].width
            val willHaveOverflow = visibleCount + 1 < tags.size
            var reservedActionsWidth = addWidth + actionSpacingPx
            if (willHaveOverflow) {
                reservedActionsWidth += moreWidth + actionSpacingPx
            }

            if (usedWidth + spacingBefore + tagWidth + reservedActionsWidth <= maxWidth) {
                usedWidth += spacingBefore + tagWidth
                visibleCount++
            } else {
                break
            }
        }

        val hasOverflow = visibleCount < tags.size
        val visiblePlaceables = tagPlaceables.take(visibleCount)
        val rowHeight = listOfNotNull(
            visiblePlaceables.maxOfOrNull { it.height },
            addPlaceable.height,
            if (hasOverflow) morePlaceable.height else null,
        ).maxOrNull() ?: 0

        layout(maxWidth, rowHeight) {
            var x = 0
            visiblePlaceables.forEachIndexed { index, placeable ->
                if (index > 0) {
                    x += tagSpacingPx
                }
                placeable.placeRelative(
                    x = x,
                    y = (rowHeight - placeable.height) / 2,
                )
                x += placeable.width
            }
            if (hasOverflow) {
                x += actionSpacingPx
                morePlaceable.placeRelative(
                    x = x,
                    y = (rowHeight - morePlaceable.height) / 2,
                )
                x += morePlaceable.width
            }
            x += actionSpacingPx
            addPlaceable.placeRelative(
                x = x,
                y = (rowHeight - addPlaceable.height) / 2,
            )
        }
    }
}

@Composable
private fun TagQuickAccessChip(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ColoredTagChip(
        name = if (tag.isUntaggedFilter) {
            stringResource(R.string.tag_untagged)
        } else {
            tag.name
        },
        color = tag.color,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun TagMoreButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    CompactFilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.action_more)) },
    )
}

@Composable
private fun TagAddButton(
    onClick: () -> Unit,
) {
    CompactFilterChip(
        selected = false,
        onClick = onClick,
        contentPadding = TagChipSpacing.AddButtonContentPadding,
        label = { Text("+") },
    )
}

@Composable
private fun TagQuickAccessActions(
    showMore: Boolean,
    tagsExpanded: Boolean,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenActionButtons)) {
        if (showMore) {
            TagMoreButton(
                selected = tagsExpanded,
                onClick = onTagsExpandedToggle,
            )
        }
        TagAddButton(onClick = onOpenTags)
    }
}
