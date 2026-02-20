package ua.danichapps.mybusinesscalendar.ui.addevent

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared screen for adding a new event and editing an existing one.
 *
 * @param initialDayMillis Pre-sets start time when adding (ignored in edit mode).
 * @param editingEventId   Non-null → edit mode; `null` → add mode.
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
    // FIX #3 — lifecycle-aware collection; stops when screen is invisible
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(editingEventId) {
        if (editingEventId != null) viewModel.loadEvent(editingEventId)
        else viewModel.setInitialDay(initialDayMillis)
    }

    // FIX #1/#2 — one-shot events from Channel; never re-triggered after config change
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEventUiEvent.NavigateBack  -> onNavigateBack()
                is AddEditEventUiEvent.ShowError     -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingEventId != null) "Edit Event" else "New Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

        // FIX #7 — EventForm receives lambdas, not the ViewModel directly
        EventForm(
            uiState                = uiState,
            onTitleChange          = viewModel::onTitleChange,
            onDescriptionChange    = viewModel::onDescriptionChange,
            onStartTimeChange      = viewModel::onStartTimeChange,
            onEndTimeChange        = viewModel::onEndTimeChange,
            onIsAllDayChange       = viewModel::onIsAllDayChange,
            onSave                 = viewModel::save,
            modifier               = Modifier.padding(padding),
        )
    }
}

// ── Form ───────────────────────────────────────────────────────────────────────

/**
 * Stateless form composable — receives all data and callbacks, holds only ephemeral
 * picker-visibility flags that have no meaning outside this composable.
 *
 * FIX #7 — accepts lambdas instead of ViewModel → testable in isolation, proper state hoisting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventForm(
    uiState: AddEditEventUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onEndTimeChange: (Long) -> Unit,
    onIsAllDayChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm",       Locale.getDefault()) }

    // Ephemeral UI state — picker visibility flags live only here, not in ViewModel
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    // FIX #8 — DatePickerState lives outside the `if` so the user's browsed
    // month/selection is preserved when the dialog is closed without confirming.
    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startTimeMillis)
    val endDatePickerState   = rememberDatePickerState(initialSelectedDateMillis = uiState.endTimeMillis)

    // TimePicker states: kept inside the `if` blocks (below) so they reset to
    // the current persisted time each time the picker opens — intentional UX.

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value          = uiState.title,
            onValueChange  = onTitleChange,
            label          = { Text("Title *") },
            modifier       = Modifier.fillMaxWidth(),
            isError        = uiState.titleError != null,
            supportingText = uiState.titleError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
            singleLine     = true,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = uiState.description,
            onValueChange = onDescriptionChange,
            label         = { Text("Description") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 3,
            maxLines      = 5,
        )

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("All day", Modifier.weight(1f))
            Switch(checked = uiState.isAllDay, onCheckedChange = onIsAllDayChange)
        }

        Spacer(Modifier.height(12.dp))

        Text("Start", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showStartDatePicker = true }) {
                Text(dateFormat.format(Date(uiState.startTimeMillis)))
            }
            if (!uiState.isAllDay) {
                TextButton(onClick = { showStartTimePicker = true }) {
                    Text(timeFormat.format(Date(uiState.startTimeMillis)))
                }
            }
        }

        Text("End", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showEndDatePicker = true }) {
                Text(dateFormat.format(Date(uiState.endTimeMillis)))
            }
            if (!uiState.isAllDay) {
                TextButton(onClick = { showEndTimePicker = true }) {
                    Text(timeFormat.format(Date(uiState.endTimeMillis)))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }

    // ── Date pickers (state lives outside → selection preserved on dismiss) ───

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

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        onEndTimeChange(mergeDateIntoMillis(it, uiState.endTimeMillis))
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(endDatePickerState) }
    }

    // ── Time pickers (state inside → always reflects current saved time on open) ─

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

    if (showEndTimePicker) {
        val endCal = Calendar.getInstance().apply { timeInMillis = uiState.endTimeMillis }
        val state  = rememberTimePickerState(
            initialHour   = endCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = endCal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndTimeChange(mergeTimeIntoMillis(uiState.endTimeMillis, state.hour, state.minute))
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") } },
            title = { Text("End time") },
            text  = { TimePicker(state) },
        )
    }
}

// ── Pure helpers ───────────────────────────────────────────────────────────────

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
