package ua.danichapps.radiantdays.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs

private const val BOLD_MARKER = "**"
private const val BULLET_PREFIX = "- "
private const val LARGE_PREFIX = "## "
private const val SMALL_PREFIX = "###### "
private const val DISPLAY_BULLET_PREFIX = "• "

enum class NoteFontSize {
    Small,
    Normal,
    Large,
}

data class NoteDisplayStyles(
    val smallSize: TextUnit,
    val normalSize: TextUnit,
    val largeSize: TextUnit,
) {
    fun fontSizeFor(size: NoteFontSize): TextUnit = when (size) {
        NoteFontSize.Small -> smallSize
        NoteFontSize.Normal -> normalSize
        NoteFontSize.Large -> largeSize
    }

    fun sizeForFontSize(fontSize: TextUnit?): NoteFontSize {
        if (fontSize == null || fontSize == TextUnit.Unspecified) return NoteFontSize.Normal
        return when {
            sizesClose(fontSize, largeSize) -> NoteFontSize.Large
            sizesClose(fontSize, smallSize) -> NoteFontSize.Small
            else -> NoteFontSize.Normal
        }
    }

    private fun sizesClose(a: TextUnit, b: TextUnit): Boolean {
        if (a == b) return true
        return abs(a.value - b.value) < 0.5f
    }
}

data class NoteFormatState(
    val isBold: Boolean = false,
    val isBullet: Boolean = false,
    val fontSize: NoteFontSize = NoteFontSize.Normal,
)

fun noteMarkdownToFieldValue(
    markdown: String,
    styles: NoteDisplayStyles,
    selection: TextRange? = null,
): TextFieldValue {
    if (markdown.isEmpty()) {
        val resolved = selection ?: TextRange.Zero
        return TextFieldValue(AnnotatedString(""), resolved.clamped(0))
    }
    val annotated = buildAnnotatedString {
        val lines = markdown.split('\n')
        lines.forEachIndexed { index, line ->
            if (index > 0) append('\n')
            val lineStart = length
            val structure = parseMarkdownLineStructure(line)
            if (structure.bullet) {
                append(DISPLAY_BULLET_PREFIX)
            }
            appendContentWithBold(structure.content)
            val lineEnd = length
            val sizeStyle = SpanStyle(fontSize = styles.fontSizeFor(structure.size))
            if (structure.size != NoteFontSize.Normal && lineEnd > lineStart) {
                addStyle(sizeStyle, lineStart, lineEnd)
            }
        }
    }
    val resolvedSelection = selection ?: TextRange(annotated.length)
    return TextFieldValue(annotated, resolvedSelection.clamped(annotated.length))
}

fun noteFieldValueToMarkdown(value: TextFieldValue, styles: NoteDisplayStyles): String {
    if (value.text.isEmpty()) return ""
    val lines = value.text.split('\n')
    var offset = 0
    return lines.joinToString("\n") { line ->
        val lineStart = offset
        offset += line.length + 1
        displayLineToMarkdown(
            line = line,
            annotated = value.annotatedString,
            lineStart = lineStart,
            styles = styles,
        )
    }
}

fun noteFormatStateAt(
    value: TextFieldValue,
    styles: NoteDisplayStyles,
    boldTyping: Boolean,
): NoteFormatState {
    val line = displayLineAt(value.text, value.selection.start)
    return NoteFormatState(
        isBold = boldTyping || isSelectionBold(value),
        isBullet = line.startsWith(DISPLAY_BULLET_PREFIX),
        fontSize = lineFontSize(value.annotatedString, value.text, value.selection.start, styles),
    )
}

fun toggleBold(
    value: TextFieldValue,
    boldTyping: Boolean,
    onBoldTypingChange: (Boolean) -> Unit,
): TextFieldValue {
    if (!value.selection.collapsed) {
        onBoldTypingChange(false)
        return value.copy(
            annotatedString = toggleBoldOnRange(value.annotatedString, value.selection),
            selection = value.selection,
        )
    }
    onBoldTypingChange(!boldTyping)
    return value
}

fun applyBoldTyping(previous: TextFieldValue, current: TextFieldValue): TextFieldValue {
    val insertAt = previous.selection.start
    val added = current.text.length - previous.text.length
    if (added <= 0 || insertAt < 0) return current
    val end = (insertAt + added).coerceAtMost(current.text.length)
    return current.copy(
        annotatedString = buildAnnotatedString {
            append(current.annotatedString)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), insertAt, end)
        },
    )
}

fun toggleBulletLine(value: TextFieldValue): TextFieldValue {
    val lineRange = lineRange(value.text, value.selection.start)
    val line = value.text.substring(lineRange)
    val newLine = if (line.startsWith(DISPLAY_BULLET_PREFIX)) {
        line.removePrefix(DISPLAY_BULLET_PREFIX)
    } else {
        DISPLAY_BULLET_PREFIX + line
    }
    return replaceDisplayLine(value, lineRange, newLine)
}

fun setLineFontSize(
    value: TextFieldValue,
    size: NoteFontSize,
    styles: NoteDisplayStyles,
): TextFieldValue {
    val lineRange = lineRange(value.text, value.selection.start)
    if (lineRange.isEmpty()) return value
    val targetSize = if (size == NoteFontSize.Normal) {
        TextUnit.Unspecified
    } else {
        styles.fontSizeFor(size)
    }
    val annotated = rebuildAnnotatedString(value.annotatedString) { index, style ->
        if (index in lineRange) style.copy(fontSize = targetSize) else style
    }
    return value.copy(annotatedString = annotated)
}

fun appendVoiceTextToRichFieldValue(current: TextFieldValue, spoken: String): TextFieldValue {
    if (spoken.isBlank()) return current

    val insertAt = current.selection.start.coerceIn(0, current.text.length)
    val prefix = when {
        current.text.isEmpty() || insertAt == 0 -> ""
        current.text[insertAt - 1].isWhitespace() -> ""
        else -> " "
    }
    val insertion = prefix + spoken
    val annotated = buildAnnotatedString {
        append(current.annotatedString.subSequence(0, insertAt))
        append(insertion)
        append(current.annotatedString.subSequence(insertAt, current.text.length))
    }
    val cursor = insertAt + insertion.length
    return TextFieldValue(annotated, TextRange(cursor))
}

private data class MarkdownLineStructure(
    val size: NoteFontSize,
    val bullet: Boolean,
    val content: String,
)

private fun parseMarkdownLineStructure(line: String): MarkdownLineStructure {
    var rest = line
    var size = NoteFontSize.Normal
    when {
        rest.startsWith(LARGE_PREFIX) -> {
            size = NoteFontSize.Large
            rest = rest.removePrefix(LARGE_PREFIX)
        }
        rest.startsWith(SMALL_PREFIX) -> {
            size = NoteFontSize.Small
            rest = rest.removePrefix(SMALL_PREFIX)
        }
    }
    val bullet = rest.startsWith(BULLET_PREFIX)
    if (bullet) {
        rest = rest.removePrefix(BULLET_PREFIX)
    }
    return MarkdownLineStructure(size = size, bullet = bullet, content = rest)
}

private fun buildMarkdownLine(structure: MarkdownLineStructure): String {
    val sizePrefix = when (structure.size) {
        NoteFontSize.Normal -> ""
        NoteFontSize.Large -> LARGE_PREFIX
        NoteFontSize.Small -> SMALL_PREFIX
    }
    val bulletPrefix = if (structure.bullet) BULLET_PREFIX else ""
    return sizePrefix + bulletPrefix + structure.content
}

private fun AnnotatedString.Builder.appendContentWithBold(content: String) {
    var index = 0
    while (index < content.length) {
        if (content.startsWith(BOLD_MARKER, index)) {
            val endMarker = content.indexOf(BOLD_MARKER, index + BOLD_MARKER.length)
            if (endMarker != -1) {
                val start = length
                append(content.substring(index + BOLD_MARKER.length, endMarker))
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                index = endMarker + BOLD_MARKER.length
                continue
            }
        }
        append(content[index])
        index++
    }
}

private fun displayLineToMarkdown(
    line: String,
    annotated: AnnotatedString,
    lineStart: Int,
    styles: NoteDisplayStyles,
): String {
    var content = line
    val bullet = content.startsWith(DISPLAY_BULLET_PREFIX)
    if (bullet) {
        content = content.removePrefix(DISPLAY_BULLET_PREFIX)
    }
    val contentStart = lineStart + if (bullet) DISPLAY_BULLET_PREFIX.length else 0
    val size = lineFontSize(annotated, line, lineStart, styles)
    val markdownContent = serializeBoldContent(annotated, contentStart, content)
    return buildMarkdownLine(
        MarkdownLineStructure(
            size = size,
            bullet = bullet,
            content = markdownContent,
        ),
    )
}

private fun serializeBoldContent(
    annotated: AnnotatedString,
    contentStart: Int,
    content: String,
): String = buildString {
    var index = 0
    while (index < content.length) {
        val absolute = contentStart + index
        val isBold = annotated.spanStyles.any { span ->
            absolute in span.start until span.end &&
                span.item.fontWeight == FontWeight.Bold
        }
        if (!isBold) {
            append(content[index])
            index++
            continue
        }
        val boldStart = index
        while (index < content.length) {
            val position = contentStart + index
            val charBold = annotated.spanStyles.any { span ->
                position in span.start until span.end &&
                    span.item.fontWeight == FontWeight.Bold
            }
            if (!charBold) break
            index++
        }
        append(BOLD_MARKER)
        append(content.substring(boldStart, index))
        append(BOLD_MARKER)
    }
}

private fun lineFontSize(
    annotated: AnnotatedString,
    text: String,
    cursor: Int,
    styles: NoteDisplayStyles,
): NoteFontSize {
    val lineRange = lineRange(text, cursor)
    if (lineRange.isEmpty()) return NoteFontSize.Normal
    val sampleIndex = lineRange.first
    val fontSize = annotated.spanStyles
        .firstOrNull { sampleIndex in it.start until it.end && it.item.fontSize != TextUnit.Unspecified }
        ?.item
        ?.fontSize
    return styles.sizeForFontSize(fontSize)
}

private fun isSelectionBold(value: TextFieldValue): Boolean {
    val selection = value.selection
    if (!selection.collapsed) {
        return (selection.min until selection.max).all { index ->
            value.annotatedString.spanStyles.any { span ->
                index in span.start until span.end &&
                    span.item.fontWeight == FontWeight.Bold
            }
        }
    }
    val cursor = selection.start.coerceIn(0, value.text.length.coerceAtLeast(0))
    if (value.text.isEmpty()) return false
    return value.annotatedString.spanStyles.any { span ->
        cursor in span.start until span.end &&
            span.item.fontWeight == FontWeight.Bold
    }
}

private fun toggleBoldOnRange(annotated: AnnotatedString, selection: TextRange): AnnotatedString {
    val start = selection.min
    val end = selection.max
    val makeBold = !(start until end).all { index ->
        annotated.spanStyles.any { span ->
            index in span.start until span.end &&
                span.item.fontWeight == FontWeight.Bold
        }
    }
    return if (makeBold) {
        buildAnnotatedString {
            append(annotated)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
        }
    } else {
        buildAnnotatedString {
            removeBoldInRange(annotated, start, end)
        }
    }
}

private fun rebuildAnnotatedString(
    source: AnnotatedString,
    transform: (Int, SpanStyle) -> SpanStyle,
): AnnotatedString = buildAnnotatedString {
    if (source.text.isEmpty()) return@buildAnnotatedString
    for (index in source.text.indices) {
        val charStart = length
        append(source.text[index])
        val merged = source.spanStyles.fold(SpanStyle()) { acc, span ->
            if (index in span.start until span.end) acc.merge(span.item) else acc
        }
        val style = transform(index, merged)
        if (style.fontWeight == FontWeight.Bold || style.fontSize != TextUnit.Unspecified) {
            addStyle(style, charStart, length)
        }
    }
}

private fun AnnotatedString.Builder.removeBoldInRange(
    source: AnnotatedString,
    start: Int,
    end: Int,
) {
    append(source.text)
    source.spanStyles.forEach { span ->
        val style = span.item
        val spanStart = span.start
        val spanEnd = span.end
        if (style.fontWeight != FontWeight.Bold) {
            addStyle(style, spanStart, spanEnd)
            return@forEach
        }
        if (spanEnd <= start || spanStart >= end) {
            addStyle(style, spanStart, spanEnd)
            return@forEach
        }
        if (spanStart < start) {
            addStyle(style, spanStart, start)
        }
        if (spanEnd > end) {
            addStyle(style, end, spanEnd)
        }
    }
}

private fun displayLineAt(text: String, cursor: Int): String {
    val range = lineRange(text, cursor)
    return text.substring(range)
}

private fun lineRange(text: String, cursor: Int): IntRange {
    val clamped = cursor.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', startIndex = (clamped - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val end = text.indexOf('\n', startIndex = clamped).let { if (it == -1) text.length else it }
    return start until end
}

private fun replaceDisplayLine(
    value: TextFieldValue,
    lineRange: IntRange,
    newLine: String,
): TextFieldValue {
    val oldLine = value.text.substring(lineRange)
    val annotated = buildAnnotatedString {
        append(value.annotatedString.subSequence(0, lineRange.first))
        val lineStart = length
        append(newLine)
        val lineEnd = length
        append(value.annotatedString.subSequence(lineRange.last + 1, value.text.length))

        val oldContentStart = lineRange.first +
            if (oldLine.startsWith(DISPLAY_BULLET_PREFIX)) DISPLAY_BULLET_PREFIX.length else 0
        val newContentStart = lineStart +
            if (newLine.startsWith(DISPLAY_BULLET_PREFIX)) DISPLAY_BULLET_PREFIX.length else 0
        val oldContent = oldLine.removePrefix(DISPLAY_BULLET_PREFIX)
        val newContent = newLine.removePrefix(DISPLAY_BULLET_PREFIX)

        value.annotatedString.spanStyles.forEach { span ->
            if (span.item.fontSize != TextUnit.Unspecified &&
                span.start < lineRange.last + 1 &&
                span.end > lineRange.first
            ) {
                addStyle(SpanStyle(fontSize = span.item.fontSize), lineStart, lineEnd)
            }
        }
        for (index in oldContent.indices) {
            if (index >= newContent.length) break
            val oldIndex = oldContentStart + index
            val newIndex = newContentStart + index
            val isBold = value.annotatedString.spanStyles.any { span ->
                oldIndex in span.start until span.end &&
                    span.item.fontWeight == FontWeight.Bold
            }
            if (isBold) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), newIndex, newIndex + 1)
            }
        }
    }
    val delta = newLine.length - oldLine.length
    val lineStart = lineRange.first
    val lineEnd = lineStart + newLine.length
    val newSelection = when {
        value.selection.collapsed -> {
            val cursor = (value.selection.start + delta).coerceIn(lineStart, lineEnd)
            TextRange(cursor)
        }
        else -> {
            val start = (value.selection.start + delta).coerceIn(lineStart, lineEnd)
            val end = (value.selection.end + delta).coerceIn(lineStart, lineEnd)
            TextRange(minOf(start, end), maxOf(start, end))
        }
    }
    return TextFieldValue(annotated, newSelection.clamped(annotated.length))
}

private fun TextRange.clamped(length: Int): TextRange {
    val start = start.coerceIn(0, length)
    val end = end.coerceIn(0, length)
    return if (collapsed) TextRange(start) else TextRange(start, end)
}
