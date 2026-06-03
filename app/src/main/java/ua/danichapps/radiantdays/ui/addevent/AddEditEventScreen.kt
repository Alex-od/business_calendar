package ua.danichapps.radiantdays.ui.addevent

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ua.danichapps.radiantdays.domain.model.Folder
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
    onOpenFolders: () -> Unit,
    createdFolderGuid: String? = null,
    onCreatedFolderGuidConsumed: () -> Unit = {},
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

    LaunchedEffect(createdFolderGuid) {
        val folderGuid = createdFolderGuid ?: return@LaunchedEffect
        viewModel.onFolderSelected(folderGuid)
        onCreatedFolderGuidConsumed()
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
                title = { Text(if (editingEventId != null) "Редактировать заметку" else "Новая заметка") },
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
            startTimeMillis = uiState.startTimeMillis,
            onFolderSelected = viewModel::onFolderSelected,
            onOpenFolders = onOpenFolders,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventForm(
    uiState: AddEditEventUiState,
    onDescriptionChange: (String) -> Unit,
    onAlarmTimeChange: (Long) -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onOpenFolders: () -> Unit,
    startTimeMillis: Long,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val pinnedFolders = uiState.folders.filter { it.isPinned }

    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var showAlarmTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        uiState.alarmTimeMillis?.let { alarmTimeMillis ->
            ReminderSection(
                alarmTimeMillis = alarmTimeMillis,
                notificationMinutesBefore = uiState.notificationMinutesBefore,
                startTimeMillis = startTimeMillis,
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

        if (pinnedFolders.isNotEmpty()) {
            PinnedFolderRow(
                folders = pinnedFolders,
                selectedFolderGuid = uiState.selectedFolderGuid,
                onFolderSelected = onFolderSelected,
            )

            Spacer(Modifier.height(12.dp))
        }

        FolderSelector(
            folders = uiState.folders,
            selectedFolderGuid = uiState.selectedFolderGuid,
            onFolderSelected = onFolderSelected,
            onOpenFolders = onOpenFolders,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Заметка *") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isError = uiState.descriptionError != null,
            supportingText = uiState.descriptionError?.let { msg ->
                { Text(msg, color = MaterialTheme.colorScheme.error) }
            },
            minLines = 12,
        )
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
    val now = System.currentTimeMillis()

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
                    onClick = { onPresetAlarm(millisPlusMinutes(now, 15)) },
                    label = { Text("+15 мин") },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(millisPlusHours(now, 1)) },
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
private fun PinnedFolderRow(
    folders: List<Folder>,
    selectedFolderGuid: String?,
    onFolderSelected: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = folders,
            key = { folder -> folder.guid },
        ) { folder ->
            FilterChip(
                selected = folder.guid == selectedFolderGuid,
                onClick = { onFolderSelected(folder.guid) },
                label = { Text(folder.name) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSelector(
    folders: List<Folder>,
    selectedFolderGuid: String?,
    onFolderSelected: (String?) -> Unit,
    onOpenFolders: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = folders.firstOrNull { it.guid == selectedFolderGuid }?.name ?: "Без папки"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                readOnly = true,
                label = { Text("Папка") },
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Без папки") },
                    onClick = {
                        onFolderSelected(null)
                        expanded = false
                    },
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.name) },
                        onClick = {
                            onFolderSelected(folder.guid)
                            expanded = false
                        },
                    )
                }
            }
        }

        IconButton(onClick = onOpenFolders) {
            Icon(Icons.Default.Add, contentDescription = "Добавить папку")
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
