package ua.danichapps.radiantdays.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteDateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

fun formatNoteDateTime(millis: Long): String = noteDateTimeFormat.format(Date(millis))
