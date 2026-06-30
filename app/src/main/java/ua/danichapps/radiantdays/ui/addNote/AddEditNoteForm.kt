package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.DrawerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Note editor inside the alarm drawer; hosts date/time pickers for the alarm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteForm(
    uiState: AddEditNoteUiState,
    callbacks: AddEditNoteScreenCallbacks,
    locale: Locale,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    onMessageClick: (Int) -> Unit = {},
) {
    val dateFormat = remember(locale) { SimpleDateFormat("dd MMM yyyy", locale) }
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val typography = MaterialTheme.typography
    val noteDisplayStyles = remember(typography) {
        NoteDisplayStyles(
            smallSize = typography.labelSmall.fontSize,
            normalSize = typography.bodyLarge.fontSize,
            largeSize = typography.headlineSmall.fontSize,
        )
    }

    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var showAlarmTimePicker by remember { mutableStateOf(false) }

    val noteEditorState = rememberNoteEditorState(
        description = uiState.description,
        editingNoteId = uiState.editingNoteId,
        locale = locale,
        noteDisplayStyles = noteDisplayStyles,
        onDescriptionChange = callbacks.onDescriptionChange,
        onDescriptionChangeFromVoice = callbacks.onDescriptionChangeFromVoice,
        onDescriptionUndo = callbacks.onDescriptionUndo,
        onVoiceInputUnavailable = callbacks.onVoiceInputUnavailable,
    )

    NotesSettingsDrawer(
        drawerState = drawerState,
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = AddEditNoteContentPadding),
        alarmTimeMillis = uiState.alarmTimeMillis,
        notificationMinutesBefore = uiState.notificationMinutesBefore,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        onAddAlarm = callbacks.onAddAlarm,
        onRemoveAlarm = callbacks.onRemoveAlarm,
        onDateClick = { showAlarmDatePicker = true },
        onTimeClick = { showAlarmTimePicker = true },
        onNotificationMinutesChange = callbacks.onNotificationMinutesChange,
        showFormatToolbar = uiState.showFormatToolbar,
        showAiChat = uiState.showAiChat,
        onShowFormatToolbarChange = callbacks.onShowFormatToolbarChange,
        onShowAiChatChange = callbacks.onShowAiChatChange,
    ) {
        NoteEditor(
            state = noteEditorState,
            uiState = uiState,
            callbacks = callbacks,
            noteDisplayStyles = noteDisplayStyles,
            onMessageClick = onMessageClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AddEditNoteContentPadding),
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
                        callbacks.onAlarmTimeChange(mergeDateIntoMillis(it, alarmTimeMillis))
                    }
                    showAlarmDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
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
                    callbacks.onAlarmTimeChange(mergeTimeIntoMillis(alarmTimeMillis, state.hour, state.minute))
                    showAlarmTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = { Text(stringResource(R.string.event_alarm_time_title)) },
            text = { TimePicker(state) },
        )
    }
}
