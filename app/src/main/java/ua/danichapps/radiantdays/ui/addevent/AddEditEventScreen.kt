package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
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
 * @param editingEventId   Non-null в†’ edit mode; `null` в†’ add mode.
 * @param onNavigateBack   Pops the back stack on save or cancel.
 * @param viewModel        Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    initialDayMillis: Long,
    editingEventId: Long?,
    onNavigateBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingEventId != null) "Edit Note" else "New Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
            uiState             = uiState,
            onDescriptionChange = viewModel::onDescriptionChange,
            onStartTimeChange   = viewModel::onStartTimeChange,
            onIsCompletedChange = viewModel::onIsCompletedChange,
            onFolderSelected    = viewModel::onFolderSelected,
            modifier            = Modifier.padding(padding),
        )
    }
}

// в”Ђв”Ђ Form в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventForm(
    uiState: AddEditEventUiState,
    onDescriptionChange: (String) -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit,
    onFolderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm",       Locale.getDefault()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startTimeMillis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        FolderSelector(
            folders = uiState.folders,
            selectedFolderGuid = uiState.selectedFolderGuid,
            onFolderSelected = onFolderSelected,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = uiState.description,
            onValueChange = onDescriptionChange,
            label         = { Text("Note *") },
            modifier      = Modifier.fillMaxWidth(),
            isError       = uiState.descriptionError != null,
            supportingText = uiState.descriptionError?.let { msg ->
                { Text(msg, color = MaterialTheme.colorScheme.error) }
            },
            minLines      = 3,
            maxLines      = 5,
        )

        Spacer(Modifier.height(16.dp))

        Text("Start", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showStartDatePicker = true }) {
                Text(dateFormat.format(Date(uiState.startTimeMillis)))
            }
            TextButton(onClick = { showStartTimePicker = true }) {
                Text(timeFormat.format(Date(uiState.startTimeMillis)))
            }
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.editingEventId != null) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked         = uiState.isCompleted,
                    onCheckedChange = onIsCompletedChange,
                )
                Text(
                    text     = "Р’С‹РїРѕР»РЅРµРЅРѕ",
                    modifier = Modifier.padding(start = 4.dp),
                    style    = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    // в”Ђв”Ђ Date picker в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let {
                        onStartTimeChange(mergeDateIntoMillis(it, uiState.startTimeMillis))
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(startDatePickerState) }
    }

    // в”Ђв”Ђ Time picker в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    if (showStartTimePicker) {
        val startCal = Calendar.getInstance().apply { timeInMillis = uiState.startTimeMillis }
        val state    = rememberTimePickerState(
            initialHour   = startCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = startCal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onStartTimeChange(mergeTimeIntoMillis(uiState.startTimeMillis, state.hour, state.minute))
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") } },
            title = { Text("Start time") },
            text  = { TimePicker(state) },
        )
    }
}

// в”Ђв”Ђ Pure helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSelector(
    folders: List<Folder>,
    selectedFolderGuid: String?,
    onFolderSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = folders.firstOrNull { it.guid == selectedFolderGuid }?.name ?: "Без папки"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
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
}

private fun mergeDateIntoMillis(dateMillis: Long, originalMillis: Long): Long {
    val dateCal     = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val originalCal = Calendar.getInstance().apply { timeInMillis = originalMillis }
    dateCal.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
    dateCal.set(Calendar.MINUTE,      originalCal.get(Calendar.MINUTE))
    dateCal.set(Calendar.SECOND,      0)
    dateCal.set(Calendar.MILLISECOND, 0)
    return dateCal.timeInMillis
}

private fun mergeTimeIntoMillis(originalMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = originalMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE,      minute)
        set(Calendar.SECOND,      0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
