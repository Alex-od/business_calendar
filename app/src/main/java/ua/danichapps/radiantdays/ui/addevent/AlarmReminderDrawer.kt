package ua.danichapps.radiantdays.ui.addevent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.R

private val AlarmDrawerWidth = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmReminderDrawer(
    alarmTimeMillis: Long?,
    notificationMinutesBefore: Int,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onAddAlarm: () -> Unit,
    onRemoveAlarm: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(AlarmDrawerWidth),
                    ) {
                        AlarmSidePanel(
                            alarmTimeMillis = alarmTimeMillis,
                            notificationMinutesBefore = notificationMinutesBefore,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            onAddAlarm = onAddAlarm,
                            onRemoveAlarm = onRemoveAlarm,
                            onDateClick = onDateClick,
                            onTimeClick = onTimeClick,
                            onNotificationMinutesChange = onNotificationMinutesChange,
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSidePanel(
    alarmTimeMillis: Long?,
    notificationMinutesBefore: Int,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onAddAlarm: () -> Unit,
    onRemoveAlarm: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.event_reminder),
            style = MaterialTheme.typography.titleSmall,
        )

        if (alarmTimeMillis == null) {
            TextButton(onClick = onAddAlarm) {
                Text(stringResource(R.string.event_add_alarm))
            }
        } else {
            val alarmMillis = alarmTimeMillis
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDateClick) {
                    Text(dateFormat.format(java.util.Date(alarmMillis)))
                }
                TextButton(onClick = onTimeClick) {
                    Text(timeFormat.format(java.util.Date(alarmMillis)))
                }
            }

            var minutesExpanded by remember { mutableStateOf(false) }
            val minutesLabel = if (notificationMinutesBefore == 0) {
                stringResource(R.string.event_on_time)
            } else {
                stringResource(R.string.event_minutes_short, notificationMinutesBefore)
            }
            AlarmDropdownField(
                label = stringResource(R.string.event_minutes_before),
                value = minutesLabel,
                expanded = minutesExpanded,
                onExpandedChange = { minutesExpanded = it },
                options = REMINDER_OFFSET_MINUTES_OPTIONS.map { minutes ->
                    val label = if (minutes == 0) {
                        stringResource(R.string.event_on_time)
                    } else {
                        stringResource(R.string.event_minutes_short, minutes)
                    }
                    label to { onNotificationMinutesChange(minutes) }
                },
            )

            TextButton(onClick = onRemoveAlarm) {
                Text(stringResource(R.string.event_remove_alarm))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<Pair<String, () -> Unit>>,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { (optionLabel, onSelect) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelect()
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
