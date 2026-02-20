package ua.danichapps.mybusinesscalendar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.koin.core.context.GlobalContext
import ua.danichapps.mybusinesscalendar.domain.model.CalendarEvent
import ua.danichapps.mybusinesscalendar.domain.model.DomainResult
import ua.danichapps.mybusinesscalendar.domain.usecase.GetUpcomingEventsUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home-screen widget that displays up to 3 upcoming events for the current day.
 *
 * Glance does not support Koin's `by inject()` directly, so dependencies are
 * resolved from [GlobalContext] (the global Koin instance).
 *
 * The widget is refreshed by the system every [android.appwidget.AppWidgetProviderInfo.updatePeriodMillis]
 * milliseconds (30 minutes in `calendar_widget_info.xml`).
 */
class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val getUpcomingEventsUseCase =
            GlobalContext.get().get<GetUpcomingEventsUseCase>()

        val now       = System.currentTimeMillis()
        val next24hMs = 24 * 60 * 60 * 1_000L

        val events = when (val result = getUpcomingEventsUseCase(now, now + next24hMs)) {
            is DomainResult.Success -> result.data
            is DomainResult.Error   -> emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(events = events)
            }
        }
    }
}

@Composable
private fun WidgetContent(events: List<CalendarEvent>) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        Text(
            text  = "Today's Events",
            style = TextStyle(
                color      = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
            ),
        )

        if (events.isEmpty()) {
            Text(
                text  = "No upcoming events",
                style = TextStyle(
                    color    = GlanceTheme.colors.onBackground,
                    fontSize = 12.sp,
                ),
            )
        } else {
            events.take(3).forEach { event ->
                val timeLabel = if (event.isAllDay) "All day"
                                else timeFormat.format(Date(event.startTimeMillis))
                Text(
                    text  = "• $timeLabel  ${event.title}",
                    style = TextStyle(
                        color    = GlanceTheme.colors.onBackground,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}
