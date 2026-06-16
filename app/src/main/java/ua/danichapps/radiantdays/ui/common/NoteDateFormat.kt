package ua.danichapps.radiantdays.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatNoteDateTime(millis: Long, locale: Locale): String {
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm", locale)
    return format.format(Date(millis))
}
