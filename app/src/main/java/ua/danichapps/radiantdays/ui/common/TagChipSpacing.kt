package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object TagChipSpacing {
    val ChipHorizontalPadding = 4.dp
    val ChipVerticalPadding = 2.dp
    val ChipContentPadding = PaddingValues(
        horizontal = ChipHorizontalPadding,
        vertical = ChipVerticalPadding,
    )

    val AddButtonExtraHorizontalPadding = 4.dp
    val AddButtonContentPadding = PaddingValues(
        horizontal = ChipHorizontalPadding + AddButtonExtraHorizontalPadding,
        vertical = ChipVerticalPadding,
    )

    val DotToLabelSpacing = 2.dp
    val BetweenTags = 2.dp
    val BetweenTagRows = 2.dp
    val BetweenActionButtons = 2.dp
    val ToolbarVerticalPadding = 4.dp
}
