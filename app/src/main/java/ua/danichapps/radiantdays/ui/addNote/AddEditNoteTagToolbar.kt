package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.ui.common.ColoredTagChip
import ua.danichapps.radiantdays.ui.common.TagChipSpacing
import ua.danichapps.radiantdays.ui.common.TagOverflowRow

/** Top bar with back button and tag quick-access chips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagToolbar(
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
) {
    Surface(
        color = TopAppBarDefaults.topAppBarColors().containerColor,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(vertical = TagChipSpacing.ToolbarVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = callbacks.onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            TagQuickAccessSection(
                modifier = Modifier.weight(1f),
                tags = uiState.tags,
                selectedTagGuids = uiState.selectedTagGuids,
                tagsExpanded = uiState.tagsExpanded,
                onTagToggle = callbacks.onTagToggle,
                onTagsExpandedToggle = callbacks.onTagsExpandedToggle,
                onOpenTags = callbacks.onOpenTags,
            )
        }
    }
}

/** Selected tags row plus expandable list of remaining tags. */
@Composable
private fun TagQuickAccessSection(
    tags: List<Tag>,
    selectedTagGuids: Set<String>,
    tagsExpanded: Boolean,
    onTagToggle: (String) -> Unit,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTags = tags.filter { it.guid in selectedTagGuids }
    val unselectedTags = tags.filter { it.guid !in selectedTagGuids }
        .sortedWith(compareByDescending<Tag> { it.isPinned }.thenBy { it.name })
    val selectedListState = rememberLazyListState()
    val tagGuids = tags.map { it.guid }

    LaunchedEffect(selectedTagGuids, tagGuids) {
        if (selectedTags.isNotEmpty()) {
            selectedListState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTagRows),
    ) {
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                state = selectedListState,
                horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTags),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                items(
                    items = selectedTags,
                    key = { tag -> "selected_${tag.guid}" },
                ) { tag ->
                    ColoredTagChip(
                        name = if (tag.isUntaggedFilter) {
                            stringResource(R.string.tag_untagged)
                        } else {
                            tag.name
                        },
                        color = tag.color,
                        selected = true,
                        onClick = { onTagToggle(tag.guid) },
                    )
                }
            }
        }
        TagOverflowRow(
            tags = unselectedTags,
            tagsExpanded = tagsExpanded,
            onTagToggle = onTagToggle,
            onTagsExpandedToggle = onTagsExpandedToggle,
            onOpenTags = onOpenTags,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    }
}
