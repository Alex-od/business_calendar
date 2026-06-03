package ua.danichapps.radiantdays.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.domain.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Constants

/** Day-of-week header labels. Sunday-first to match [Calendar.DAY_OF_WEEK]. */
private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// Screen

/**
 * Root composable for the calendar screen.
 *
 * @param onAddEvent  Called with selected-day epoch ms when the FAB is tapped.
 * @param onEditEvent Called with the event ID when "Edit" is tapped on a card.
 * @param viewModel   Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    // FIX #3: collectAsStateWithLifecycle stops collection when the screen is not visible
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // FIX #1/#2: consume one-shot events from the Channel (not from UiState fields)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                onOpenSettings = onOpenSettings,
                onOpenFolders  = onOpenFolders,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddEvent(uiState.selectedDayMillis) }) {
                Icon(Icons.Default.Add, contentDescription = "Add event")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.swipeMonths(
                    onPreviousMonth = viewModel::navigateToPreviousMonth,
                    onNextMonth     = viewModel::navigateToNextMonth,
                ),
            ) {
                MonthHeader(
                    currentMonthMillis = uiState.currentMonthMillis,
                    onPrevious         = viewModel::navigateToPreviousMonth,
                    onNext             = viewModel::navigateToNextMonth,
                )
                WeekDayHeaders()
                MonthGrid(
                    currentMonthMillis = uiState.currentMonthMillis,
                    selectedDayMillis  = uiState.selectedDayMillis,
                    eventsForMonth     = uiState.eventsForMonth,
                    onDaySelected      = viewModel::selectDay,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    EventListForDay(
                        events        = uiState.eventsForDay,
                        onEditEvent   = onEditEvent,
                        onDeleteEvent = viewModel::deleteEvent,
                    )
                }
            }
        }
    }
}

// Sub-composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    onOpenSettings: () -> Unit,
    onOpenFolders: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Open menu")
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        isMenuExpanded = false
                        onOpenSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Folders") },
                    onClick = {
                        isMenuExpanded = false
                        onOpenFolders()
                    },
                )
            }
        },
    )
}

@Composable
private fun MonthHeader(
    currentMonthMillis: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val monthLabel = remember(currentMonthMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
    }
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(text = monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekDayHeaders() {
    // FIX #6: DAY_LABELS is a top-level val, no allocation here
    Row(Modifier.fillMaxWidth()) {
        DAY_LABELS.forEach { label ->
            Text(
                text      = label,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonthMillis: Long,
    selectedDayMillis: Long,
    eventsForMonth: Map<Long, List<CalendarEvent>>,
    onDaySelected: (Long) -> Unit,
) {
    val days = remember(currentMonthMillis) { buildMonthDays(currentMonthMillis) }
    val weeks = remember(days) { days.chunked(7) }

    val selectedCal = remember(selectedDayMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
    }
    // todayCal in remember so it's not re-allocated on every recomposition
    val todayCal = remember { Calendar.getInstance() }

    Column(Modifier.fillMaxWidth()) {
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (day) {
                            is CalendarDay.Empty -> { /* intentionally empty */ }
                            is CalendarDay.Day   -> {
                                val isSelected = sameDay(selectedCal, day.millis)
                                val isToday    = sameDay(todayCal, day.millis)
                                val dayEvents  = eventsForMonth[day.millis] ?: emptyList()
                                val hasAlarm   = dayEvents.any { it.alarmTimeMillis != null }

                                val bgColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday    -> MaterialTheme.colorScheme.secondaryContainer
                                    else       -> Color.Transparent
                                }
                                val numberColor = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                                val eventColor = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                else
                                    MaterialTheme.colorScheme.primary

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bgColor)
                                        .clickable { onDaySelected(day.millis) }
                                        .padding(horizontal = 3.dp, vertical = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text       = day.number.toString(),
                                            style      = MaterialTheme.typography.bodySmall.copy(
                                                lineHeight      = 12.sp,
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim      = LineHeightStyle.Trim.Both,
                                                ),
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color      = numberColor,
                                            textAlign  = TextAlign.Center,
                                        )
                                        if (hasAlarm) {
                                            Icon(
                                                imageVector = Icons.Default.Alarm,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .padding(start = 2.dp)
                                                    .size(10.dp),
                                                tint = numberColor,
                                            )
                                        }
                                    }
                                    // Event summaries (up to 2): tight leading
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayEvents.take(2).forEach { event ->
                                            Text(
                                                text     = event.description,
                                                style    = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize        = 9.sp,
                                                    lineHeight      = 10.sp,
                                                    lineHeightStyle = LineHeightStyle(
                                                        alignment = LineHeightStyle.Alignment.Center,
                                                        trim      = LineHeightStyle.Trim.Both,
                                                    ),
                                                ),
                                                color    = eventColor,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EventListForDay(
    events: List<CalendarEvent>,
    onEditEvent: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit,
) {
    if (events.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "No events for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event    = event,
                onEdit   = { onEditEvent(event.id) },
                onDelete = { onDeleteEvent(event.id) },
            )
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // SimpleDateFormat is not thread-safe; one instance per composable is safe in single-threaded Compose
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val contentAlpha = if (event.isCompleted) 0.5f else 1f

    Card(
        modifier  = Modifier.fillMaxWidth().alpha(contentAlpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.alarmTimeMillis != null) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text       = event.description,
                        style      = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (event.isCompleted) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
                if (!event.isAllDay) {
                    Text(
                        text  = "${timeFormat.format(Date(event.startTimeMillis))} - ${timeFormat.format(Date(event.endTimeMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text  = "All day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit event")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete event",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Helpers

/** Swipe left → next month; swipe right → previous month. */
private fun Modifier.swipeMonths(
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    swipeThresholdPx: Float = 80f,
): Modifier = pointerInput(onPreviousMonth, onNextMonth) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            when {
                totalDrag <= -swipeThresholdPx -> onNextMonth()
                totalDrag >= swipeThresholdPx -> onPreviousMonth()
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
    )
}

private sealed interface CalendarDay {
    data object Empty : CalendarDay
    data class Day(val number: Int, val millis: Long) : CalendarDay
}

private fun buildMonthDays(monthMillis: Long): List<CalendarDay> {
    val cal   = Calendar.getInstance().apply { timeInMillis = monthMillis }
    val year  = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    cal.set(year, month, 1)
    val firstWeekDay = cal.get(Calendar.DAY_OF_WEEK)
    val daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<CalendarDay>()
    repeat(firstWeekDay - 1) { days += CalendarDay.Empty }
    for (day in 1..daysInMonth) {
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        days += CalendarDay.Day(day, cal.timeInMillis)
    }
    return days
}

private fun sameDay(refCal: Calendar, millis: Long): Boolean {
    val other = Calendar.getInstance().apply { timeInMillis = millis }
    return refCal.get(Calendar.YEAR)        == other.get(Calendar.YEAR) &&
           refCal.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}
