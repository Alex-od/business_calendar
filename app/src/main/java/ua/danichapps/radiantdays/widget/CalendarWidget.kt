package ua.danichapps.radiantdays.widget

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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import ua.danichapps.radiantdays.calendar.CalendarDay
import ua.danichapps.radiantdays.calendar.DAY_LABELS
import ua.danichapps.radiantdays.calendar.buildMonthDays
import ua.danichapps.radiantdays.calendar.monthWindow
import ua.danichapps.radiantdays.calendar.normaliseToDayStart
import ua.danichapps.radiantdays.calendar.sameDay
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import ua.danichapps.radiantdays.domain.model.DomainResult
import ua.danichapps.radiantdays.domain.model.ReminderPolicy
import ua.danichapps.radiantdays.domain.model.displayHeadline
import ua.danichapps.radiantdays.domain.usecase.GetEventsForMonthUseCase
import ua.danichapps.radiantdays.domain.usecase.GetUpcomingEventsUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Home-screen widget: current-month calendar grid with note headlines and
 * up to 3 upcoming reminders in the next 24 hours.
 */
class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val getEventsForMonthUseCase = koin.get<GetEventsForMonthUseCase>()
        val getUpcomingEventsUseCase = koin.get<GetUpcomingEventsUseCase>()

        val now = System.currentTimeMillis()
        val next24hMs = 24 * 60 * 60 * 1_000L
        val (monthStart, monthEnd) = monthWindow(now)

        val eventsForMonth = getEventsForMonthUseCase(monthStart, monthEnd)
            .first()
            .groupBy { normaliseToDayStart(it.startTimeMillis) }

        val reminders = when (val result = getUpcomingEventsUseCase(now, now + next24hMs)) {
            is DomainResult.Success -> result.data
            is DomainResult.Error -> emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    currentMonthMillis = now,
                    eventsForMonth = eventsForMonth,
                    reminders = reminders,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    currentMonthMillis: Long,
    eventsForMonth: Map<Long, List<CalendarEvent>>,
    reminders: List<CalendarEvent>,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(8.dp),
    ) {
        WidgetMonthHeader(currentMonthMillis = currentMonthMillis)
        WidgetWeekDayHeaders()
        WidgetMonthGrid(
            currentMonthMillis = currentMonthMillis,
            eventsForMonth = eventsForMonth,
        )
        Spacer(GlanceModifier.height(8.dp))
        WidgetRemindersSection(reminders = reminders)
    }
}

@Composable
private fun WidgetMonthHeader(currentMonthMillis: Long) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        .format(Date(currentMonthMillis))
    Text(
        text = monthLabel,
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        ),
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    )
}

@Composable
private fun WidgetWeekDayHeaders() {
    Row(GlanceModifier.fillMaxWidth()) {
        DAY_LABELS.forEach { label ->
            Text(
                text = label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun WidgetMonthGrid(
    currentMonthMillis: Long,
    eventsForMonth: Map<Long, List<CalendarEvent>>,
) {
    val days = buildMonthDays(currentMonthMillis)
    val weeks = days.chunked(7)
    val todayCal = Calendar.getInstance()

    Column(GlanceModifier.fillMaxWidth()) {
        weeks.forEach { week ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                week.forEach { day ->
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                    ) {
                        when (day) {
                            is CalendarDay.Empty -> Unit
                            is CalendarDay.Day -> WidgetDayCell(
                                day = day,
                                isToday = sameDay(todayCal, day.millis),
                                dayEvents = eventsForMonth[day.millis] ?: emptyList(),
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun WidgetDayCell(
    day: CalendarDay.Day,
    isToday: Boolean,
    dayEvents: List<CalendarEvent>,
) {
    val hasAlarm = dayEvents.any { it.alarmTimeMillis != null }
    val bgModifier = if (isToday) {
        GlanceModifier.background(GlanceTheme.colors.secondaryContainer)
    } else {
        GlanceModifier
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(bgModifier)
            .padding(horizontal = 1.dp, vertical = 1.dp),
        horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally,
    ) {
        Row {
            Text(
                text = day.number.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                ),
            )
            if (hasAlarm) {
                Text(
                    text = "⏰",
                    style = TextStyle(fontSize = 8.sp),
                    modifier = GlanceModifier.padding(start = 1.dp),
                )
            }
        }
        dayEvents.take(2).forEach { event ->
            Text(
                text = event.displayHeadline(),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 8.sp,
                ),
                maxLines = 2,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WidgetRemindersSection(reminders: List<CalendarEvent>) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Text(
        text = "Напоминания",
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        ),
    )

    if (reminders.isEmpty()) {
        Text(
            text = "Нет предстоящих",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 11.sp,
            ),
            modifier = GlanceModifier.padding(top = 2.dp),
        )
    } else {
        reminders.take(3).forEach { event ->
            val fireMillis = ReminderPolicy.reminderFireTimeMillis(event)
                ?: event.alarmTimeMillis
                ?: return@forEach
            val timeLabel = timeFormat.format(Date(fireMillis))
            Text(
                text = "• $timeLabel  ${event.displayHeadline()}",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 2.dp),
            )
        }
    }
}
