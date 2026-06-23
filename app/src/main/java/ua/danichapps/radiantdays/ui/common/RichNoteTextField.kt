package ua.danichapps.radiantdays.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RichNoteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    scrollEnabled: Boolean = true,
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val density = LocalDensity.current
    val imeBottomPx = KeyboardInsetsPolicy.rememberImeBottomPx()
    val scrollPaddingPx = with(density) { CursorScrollPadding.roundToPx().toFloat() }
    val colorScheme = MaterialTheme.colorScheme

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerHeightPx by remember { mutableIntStateOf(0) }
    var fieldBottomInRootPx by remember { mutableFloatStateOf(0f) }

    val keyboardTop = view.rootView.height - imeBottomPx
    val keyboardOverlapPx = (fieldBottomInRootPx - keyboardTop).coerceAtLeast(0f)
    val visibleHeightPx = (containerHeightPx - keyboardOverlapPx - scrollPaddingPx).coerceAtLeast(0f)

    val resolvedTextStyle = if (textStyle.color == Color.Unspecified) {
        textStyle.copy(color = colorScheme.onSurface)
    } else {
        textStyle
    }
    val selectionColors = TextSelectionColors(
        handleColor = colorScheme.primary,
        backgroundColor = colorScheme.primary.copy(alpha = 0.35f),
    )

    LaunchedEffect(value.selection, textLayoutResult, containerHeightPx, imeBottomPx, keyboardOverlapPx, scrollEnabled) {
        if (!scrollEnabled) return@LaunchedEffect
        val layout = textLayoutResult ?: return@LaunchedEffect
        scrollToCursorIfNeeded(
            scrollState = scrollState,
            layout = layout,
            cursorOffset = value.selection.start,
            visibleHeightPx = visibleHeightPx,
            scrollPaddingPx = scrollPaddingPx,
        )
    }

    val scrollModifier = if (scrollEnabled) {
        Modifier
            .onSizeChanged { containerHeightPx = it.height }
            .onGloballyPositioned { fieldBottomInRootPx = it.boundsInRoot().bottom }
            .verticalScroll(scrollState)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            onTextLayout = { textLayoutResult = it },
            textStyle = resolvedTextStyle,
            cursorBrush = SolidColor(colorScheme.primary),
            modifier = modifier
                .then(scrollModifier)
                .onFocusChanged { state -> onFocusChange(state.isFocused) },
            minLines = minLines,
        )
    }
}

private val CursorScrollPadding = 12.dp

private suspend fun scrollToCursorIfNeeded(
    scrollState: ScrollState,
    layout: TextLayoutResult,
    cursorOffset: Int,
    visibleHeightPx: Float,
    scrollPaddingPx: Float,
) {
    if (visibleHeightPx <= 0f) return

    val targetScroll = computeScrollToCursor(
        scrollState = scrollState,
        layout = layout,
        cursorOffset = cursorOffset,
        visibleHeightPx = visibleHeightPx,
        scrollPaddingPx = scrollPaddingPx,
    )
    if (targetScroll != scrollState.value) {
        scrollState.scrollTo(targetScroll)
    }
}

private fun computeScrollToCursor(
    scrollState: ScrollState,
    layout: TextLayoutResult,
    cursorOffset: Int,
    visibleHeightPx: Float,
    scrollPaddingPx: Float,
): Int {
    val textLength = layout.layoutInput.text.length
    val offset = cursorOffset.coerceIn(0, textLength)
    val lineIndex = layout.getLineForOffset(offset)
    val lineTop = layout.getLineTop(lineIndex)
    val lineBottom = layout.getLineBottom(lineIndex)

    val scrollOffset = scrollState.value.toFloat()
    val visibleTop = scrollOffset + scrollPaddingPx
    val visibleBottom = scrollOffset + visibleHeightPx - scrollPaddingPx

    return when {
        lineBottom > visibleBottom -> {
            (lineBottom + scrollPaddingPx - visibleHeightPx).roundToInt().coerceAtLeast(0)
        }
        lineTop < visibleTop -> {
            (lineTop - scrollPaddingPx).roundToInt().coerceAtLeast(0)
        }
        else -> scrollState.value
    }
}
