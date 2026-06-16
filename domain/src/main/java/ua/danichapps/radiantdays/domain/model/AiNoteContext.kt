package ua.danichapps.radiantdays.domain.model

import java.util.Locale

/** Note fields substituted into AI action prompt templates. */
data class AiNoteContext(
    val text: String,
    val title: String = "",
    val tagNames: List<String> = emptyList(),
    val noteDateMillis: Long,
    val locale: Locale = Locale.getDefault(),
)
