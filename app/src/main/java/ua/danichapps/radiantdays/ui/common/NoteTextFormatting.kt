package ua.danichapps.radiantdays.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private const val BOLD_MARKER = "**"
private const val BULLET_PREFIX = "- "
private const val LARGE_PREFIX = "## "
private const val SMALL_PREFIX = "###### "
private const val BULLET_ANNOTATION = "note_bullet"
private val BULLET_INDENT = 28.sp

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
            appendContentWithBold(structure.content)
            val lineEnd = length
            if (structure.bullet && lineEnd > lineStart) {
                addStringAnnotation(BULLET_ANNOTATION, "true", lineStart, lineEnd)
                addStyle(
                    ParagraphStyle(textIndent = TextIndent(firstLine = BULLET_INDENT, restLine = BULLET_INDENT)),
                    lineStart,
                    lineEnd,
                )
            }
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
        val lineEnd = lineStart + line.length
        offset = lineEnd + 1
        displayLineToMarkdown(
            line = line,
            annotated = value.annotatedString,
            lineStart = lineStart,
            lineEnd = lineEnd,
            styles = styles,
        )
    }
}

fun noteFormatStateAt(
    value: TextFieldValue,
    styles: NoteDisplayStyles,
    boldTyping: Boolean,
): NoteFormatState {
    val lineRange = lineRange(value.text, value.selection.start)
    return NoteFormatState(
        isBold = boldTyping || isSelectionBold(value),
        isBullet = lineHasBullet(value.annotatedString, lineRange),
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
    if (lineRange.isEmpty()) return value
    val annotated = if (lineHasBullet(value.annotatedString, lineRange)) {
        removeBulletFromLine(value.annotatedString, lineRange)
    } else {
        buildAnnotatedString {
            append(value.annotatedString)
            addStringAnnotation(BULLET_ANNOTATION, "true", lineRange.first, lineRange.last + 1)
            addStyle(
                ParagraphStyle(textIndent = TextIndent(firstLine = BULLET_INDENT, restLine = BULLET_INDENT)),
                lineRange.first,
                lineRange.last + 1,
            )
        }
    }
    return value.copy(annotatedString = annotated)
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

fun preserveSpansOnEdit(previous: TextFieldValue, incoming: TextFieldValue): TextFieldValue {
    if (previous.text == incoming.text) {
        return incoming.copy(
            annotatedString = previous.annotatedString,
            selection = incoming.selection,
        )
    }
    if (previous.annotatedString.spanStyles.isEmpty() &&
        previous.annotatedString.paragraphStyles.isEmpty() &&
        previous.annotatedString.getStringAnnotations(0, previous.text.length).isEmpty()
    ) {
        return incoming
    }
    if (incoming.annotatedString.spanStyles.isNotEmpty() ||
        incoming.annotatedString.paragraphStyles.isNotEmpty()
    ) {
        return incoming
    }
    val mapped = mapAnnotationsAfterEdit(previous.annotatedString, previous.text, incoming.text)
    return incoming.copy(annotatedString = mapped)
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
    lineEnd: Int,
    styles: NoteDisplayStyles,
): String {
    val bullet = lineHasBullet(annotated, lineStart until lineEnd)
    val size = lineFontSize(annotated, line, lineStart, styles)
    val markdownContent = serializeBoldContent(annotated, lineStart, line)
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

private fun lineHasBullet(annotated: AnnotatedString, lineRange: IntRange): Boolean =
    annotated.getStringAnnotations(BULLET_ANNOTATION, lineRange.first, lineRange.last + 1).isNotEmpty()

private fun removeBulletFromLine(annotated: AnnotatedString, lineRange: IntRange): AnnotatedString =
    buildAnnotatedString {
        append(annotated.text)
        annotated.spanStyles.forEach { span ->
            addStyle(span.item, span.start, span.end)
        }
        annotated.paragraphStyles.forEach { span ->
            val overlapsLine = span.start < lineRange.last + 1 && span.end > lineRange.first
            if (!overlapsLine) {
                addStyle(span.item, span.start, span.end)
            }
        }
        annotated.getStringAnnotations(0, annotated.length).forEach { annotation ->
            val onLine = annotation.tag == BULLET_ANNOTATION &&
                annotation.start >= lineRange.first &&
                annotation.end <= lineRange.last + 1
            if (!onLine) {
                addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
            }
        }
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

private fun mapAnnotationsAfterEdit(
    source: AnnotatedString,
    oldText: String,
    newText: String,
): AnnotatedString {
    val prefix = oldText.commonPrefixWith(newText)
    val suffix = oldText.commonSuffixWith(newText)
    val editStart = prefix.length
    val editEndOld = oldText.length - suffix.length
    val editEndNew = newText.length - suffix.length
    val delta = editEndNew - editEndOld

    return buildAnnotatedString {
        append(newText)
        source.spanStyles.forEach { span ->
            val style = span.item
            mapRange(span.start, span.end, editStart, editEndOld, editEndNew, delta).forEach { range ->
                val rangeStart = range.first
                val rangeEnd = range.last + 1
                if (rangeEnd > rangeStart) addStyle(style, rangeStart, rangeEnd)
            }
        }
        source.paragraphStyles.forEach { span ->
            val style = span.item
            mapRange(span.start, span.end, editStart, editEndOld, editEndNew, delta).forEach { range ->
                val rangeStart = range.first
                val rangeEnd = range.last + 1
                if (rangeEnd > rangeStart) addStyle(style, rangeStart, rangeEnd)
            }
        }
        source.getStringAnnotations(0, source.length).forEach { annotation ->
            mapRange(annotation.start, annotation.end, editStart, editEndOld, editEndNew, delta).forEach { range ->
                val rangeStart = range.first
                val rangeEnd = range.last + 1
                if (rangeEnd > rangeStart) {
                    addStringAnnotation(annotation.tag, annotation.item, rangeStart, rangeEnd)
                }
            }
        }
    }
}

private fun mapRange(
    start: Int,
    end: Int,
    editStart: Int,
    editEndOld: Int,
    editEndNew: Int,
    delta: Int,
): List<IntRange> {
    if (end <= editStart) return listOf(start until end)
    if (start >= editEndOld) return listOf((start + delta) until (end + delta))
    val ranges = mutableListOf<IntRange>()
    if (start < editStart) ranges += start until editStart
    ranges += editStart until editEndNew
    if (end > editEndOld) ranges += (editEndNew + (end - editEndOld)) until (end + delta)
    return ranges
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
    source.paragraphStyles.forEach { span ->
        addStyle(span.item, span.start, span.end)
    }
    source.getStringAnnotations(0, source.length).forEach { annotation ->
        addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
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
    source.paragraphStyles.forEach { span ->
        addStyle(span.item, span.start, span.end)
    }
    source.getStringAnnotations(0, source.length).forEach { annotation ->
        addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
    }
}

private fun lineRange(text: String, cursor: Int): IntRange {
    val clamped = cursor.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', startIndex = (clamped - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val end = text.indexOf('\n', startIndex = clamped).let { if (it == -1) text.length else it }
    return start until end
}

private fun TextRange.clamped(length: Int): TextRange {
    val start = start.coerceIn(0, length)
    val end = end.coerceIn(0, length)
    return if (collapsed) TextRange(start) else TextRange(start, end)
}
