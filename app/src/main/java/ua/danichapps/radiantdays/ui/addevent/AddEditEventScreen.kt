package ua.danichapps.radiantdays.ui.addevent

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.ui.common.ColoredTagChip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared screen for adding a new event and editing an existing one.
 *
 * @param initialDayMillis Pre-sets start time when adding (ignored in edit mode).
 * @param editingEventId   Non-null -> edit mode; `null` -> add mode.
 * @param onNavigateBack   Pops the back stack on save or cancel.
 * @param viewModel        Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    initialDayMillis: Long,
    editingEventId: Long?,
    onNavigateBack: () -> Unit,
    onOpenTags: () -> Unit,
    createdTagGuid: String? = null,
    onCreatedTagGuidConsumed: () -> Unit = {},
    viewModel: AddEditEventViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(editingEventId) {
        if (editingEventId != null) viewModel.loadEvent(editingEventId)
        else viewModel.setInitialDay(initialDayMillis)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEventUiEvent.NavigateBack -> onNavigateBack()
                is AddEditEventUiEvent.ShowError    -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(createdTagGuid) {
        val tagGuid = createdTagGuid ?: return@LaunchedEffect
        viewModel.onTagAddedFromSettings(tagGuid)
        onCreatedTagGuidConsumed()
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> viewModel.onAddAlarmClick() }

    fun requestAlarmWithPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onAddAlarmClick()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onAddAlarmClick()
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        placeholder = { Text("Заголовок") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = borderlessTextFieldColors(),
                        isError = uiState.titleError != null,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState.alarmTimeMillis == null) {
                        IconButton(onClick = { requestAlarmWithPermission() }) {
                            Icon(Icons.Default.AlarmAdd, contentDescription = "Добавить будильник")
                        }
                    } else {
                        IconButton(onClick = viewModel::onRemoveAlarmClick) {
                            Icon(Icons.Default.AlarmOff, contentDescription = "Удалить будильник")
                        }
                    }
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(64.dp))
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        EventForm(
            uiState = uiState,
            onDescriptionChange = viewModel::onDescriptionChange,
            onAlarmTimeChange = viewModel::onAlarmTimeChange,
            onNotificationMinutesChange = viewModel::onNotificationMinutesChange,
            onIsCompletedChange = viewModel::onIsCompletedChange,
            onTagToggle = viewModel::onTagToggle,
            onTagsExpandedToggle = viewModel::onTagsExpandedToggle,
            onOpenTags = onOpenTags,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun borderlessTextFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EventForm(
    uiState: AddEditEventUiState,
    onDescriptionChange: (String) -> Unit,
    onAlarmTimeChange: (Long) -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bodyScrollState = rememberScrollState()
    val bodyTextStyle = MaterialTheme.typography.bodyLarge
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var showAlarmTimePicker by remember { mutableStateOf(false) }

    var descriptionValue by remember { mutableStateOf(TextFieldValue(uiState.description)) }
    var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    // Sync text when description changes externally (e.g. after loadEvent).
    LaunchedEffect(uiState.description) {
        if (uiState.description != descriptionValue.text) {
            descriptionValue = TextFieldValue(
                text = uiState.description,
                selection = TextRange(uiState.description.length),
            )
        }
    }

    // Bring cursor rect into view whenever cursor position changes.
    LaunchedEffect(descriptionValue.selection) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (descriptionValue.text.isEmpty()) return@LaunchedEffect
        val cursorOffset = descriptionValue.selection.start.coerceIn(0, descriptionValue.text.length)
        bringIntoViewRequester.bringIntoView(layout.getCursorRect(cursorOffset))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(bodyScrollState)
            .padding(16.dp),
    ) {
        uiState.alarmTimeMillis?.let { alarmTimeMillis ->
            ReminderSection(
                alarmTimeMillis = alarmTimeMillis,
                notificationMinutesBefore = uiState.notificationMinutesBefore,
                startTimeMillis = uiState.startTimeMillis,
                isCompleted = uiState.isCompleted,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                onDateClick = { showAlarmDatePicker = true },
                onTimeClick = { showAlarmTimePicker = true },
                onPresetAlarm = onAlarmTimeChange,
                onNotificationMinutesChange = onNotificationMinutesChange,
                onIsCompletedChange = onIsCompletedChange,
            )

            Spacer(Modifier.height(12.dp))
        }

        TagQuickAccessSection(
            tags = uiState.tags,
            selectedTagGuids = uiState.selectedTagGuids,
            tagsExpanded = uiState.tagsExpanded,
            onTagToggle = onTagToggle,
            onTagsExpandedToggle = onTagsExpandedToggle,
            onOpenTags = onOpenTags,
        )

        Spacer(Modifier.height(12.dp))

        BasicTextField(
            value = descriptionValue,
            onValueChange = { newValue ->
                descriptionValue = newValue
                onDescriptionChange(newValue.text)
            },
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
            textStyle = bodyTextStyle.copy(color = bodyColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )

        if (uiState.descriptionError != null) {
            Text(
                text = uiState.descriptionError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    val alarmTimeMillis = uiState.alarmTimeMillis

    if (showAlarmDatePicker && alarmTimeMillis != null) {
        val alarmDatePickerState = rememberDatePickerState(initialSelectedDateMillis = alarmTimeMillis)
        DatePickerDialog(
            onDismissRequest = { showAlarmDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    alarmDatePickerState.selectedDateMillis?.let {
                        onAlarmTimeChange(mergeDateIntoMillis(it, alarmTimeMillis))
                    }
                    showAlarmDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showAlarmDatePicker = false }) { Text("Отмена") } },
        ) { DatePicker(alarmDatePickerState) }
    }

    if (showAlarmTimePicker && alarmTimeMillis != null) {
        val alarmCal = Calendar.getInstance().apply { timeInMillis = alarmTimeMillis }
        val state = rememberTimePickerState(
            initialHour = alarmCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = alarmCal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showAlarmTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onAlarmTimeChange(mergeTimeIntoMillis(alarmTimeMillis, state.hour, state.minute))
                    showAlarmTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showAlarmTimePicker = false }) { Text("Отмена") } },
            title = { Text("Время будильника") },
            text = { TimePicker(state) },
        )
    }
}

@Composable
private fun ReminderSection(
    alarmTimeMillis: Long,
    notificationMinutesBefore: Int,
    startTimeMillis: Long,
    isCompleted: Boolean,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPresetAlarm: (Long) -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit,
) {
    val fireMillis = reminderFireTimeMillis(alarmTimeMillis, notificationMinutesBefore)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Alarm, contentDescription = null)
            Column(Modifier.padding(start = 8.dp)) {
                Text("Напоминание", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Push: ${dateFormat.format(Date(fireMillis))} ${timeFormat.format(Date(fireMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDateClick) {
                Text(dateFormat.format(Date(alarmTimeMillis)))
            }
            TextButton(onClick = onTimeClick) {
                Text(timeFormat.format(Date(alarmTimeMillis)))
            }
        }
        Text("Быстрый выбор", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(millisPlusMinutes(System.currentTimeMillis(), 15)) },
                    label = { Text("+15 мин") },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(millisPlusHours(System.currentTimeMillis(), 1)) },
                    label = { Text("+1 ч") },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(tomorrowAtNineMillis()) },
                    label = { Text("Завтра 9:00") },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(noteDayAtNineMillis(startTimeMillis)) },
                    label = { Text("День заметки 9:00") },
                )
            }
        }
        Text("За сколько минут до напоминания", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(REMINDER_OFFSET_MINUTES_OPTIONS) { minutes ->
                FilterChip(
                    selected = notificationMinutesBefore == minutes,
                    onClick = { onNotificationMinutesChange(minutes) },
                    label = {
                        Text(if (minutes == 0) "В срок" else "$minutes мин")
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isCompleted, onCheckedChange = onIsCompletedChange)
            Text("Выполнено", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TagQuickAccessSection(
    tags: List<Tag>,
    selectedTagGuids: Set<String>,
    tagsExpanded: Boolean,
    onTagToggle: (String) -> Unit,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
) {
    val selectedTags = tags.filter { it.guid in selectedTagGuids }
    val pinnedUnselectedTags = tags.filter { it.isPinned && it.guid !in selectedTagGuids }
    val otherUnselectedTags = tags.filter { !it.isPinned && it.guid !in selectedTagGuids }
    val hasMoreTags = otherUnselectedTags.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Теги", style = MaterialTheme.typography.labelMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = selectedTags,
                key = { tag -> "selected_${tag.guid}" },
            ) { tag ->
                ColoredTagChip(
                    name = tag.name,
                    color = tag.color,
                    selected = true,
                    onClick = { onTagToggle(tag.guid) },
                )
            }
            items(
                items = pinnedUnselectedTags,
                key = { tag -> tag.guid },
            ) { tag ->
                ColoredTagChip(
                    name = tag.name,
                    color = tag.color,
                    selected = tag.guid in selectedTagGuids,
                    onClick = { onTagToggle(tag.guid) },
                )
            }
            if (hasMoreTags) {
                item(key = "more") {
                    FilterChip(
                        selected = tagsExpanded,
                        onClick = onTagsExpandedToggle,
                        label = { Text("Ещё") },
                    )
                }
            }
            if (tagsExpanded) {
                items(
                    items = otherUnselectedTags,
                    key = { tag -> tag.guid },
                ) { tag ->
                    ColoredTagChip(
                        name = tag.name,
                        color = tag.color,
                        selected = tag.guid in selectedTagGuids,
                        onClick = { onTagToggle(tag.guid) },
                    )
                }
            }
            item(key = "add_tag") {
                FilterChip(
                    selected = false,
                    onClick = onOpenTags,
                    label = { Text("+") },
                )
            }
        }
    }
}

private fun mergeDateIntoMillis(dateMillis: Long, originalMillis: Long): Long {
    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val originalCal = Calendar.getInstance().apply { timeInMillis = originalMillis }
    dateCal.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
    dateCal.set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
    dateCal.set(Calendar.SECOND, 0)
    dateCal.set(Calendar.MILLISECOND, 0)
    return dateCal.timeInMillis
}

private fun mergeTimeIntoMillis(originalMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = originalMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

