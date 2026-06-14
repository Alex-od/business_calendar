package ua.danichapps.radiantdays.ui.common

import androidx.compose.ui.graphics.Color
import ua.danichapps.radiantdays.domain.model.EventColor

fun EventColor.toComposeColor(): Color = when (this) {
    EventColor.DEFAULT -> Color(0xFF9E9E9E)
    EventColor.RED -> Color(0xFFE53935)
    EventColor.ORANGE -> Color(0xFFFB8C00)
    EventColor.YELLOW -> Color(0xFFF9A825)
    EventColor.GREEN -> Color(0xFF43A047)
    EventColor.BLUE -> Color(0xFF1E88E5)
    EventColor.PURPLE -> Color(0xFF8E24AA)
}
