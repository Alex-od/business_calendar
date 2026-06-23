package ua.danichapps.radiantdays.ui.addevent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import ua.danichapps.radiantdays.ui.settings.AiApiLogScreen
import ua.danichapps.radiantdays.ui.settings.DebugAiLogsSideMenuItem

private val AlarmDrawerWidth = 280.dp

/** Right-side drawer for alarm settings and note editor toggles. */
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
    showFormatToolbar: Boolean,
    showAiChat: Boolean,
    onShowFormatToolbarChange: (Boolean) -> Unit,
    onShowAiChatChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAiLogScreen by remember { mutableStateOf(false) }

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
                            showFormatToolbar = showFormatToolbar,
                            showAiChat = showAiChat,
                            onShowFormatToolbarChange = onShowFormatToolbarChange,
                            onShowAiChatChange = onShowAiChatChange,
                            onShowAiLogs = {
                                scope.launch {
                                    drawerState.close()
                                    showAiLogScreen = true
                                }
                            },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    if (showAiLogScreen) {
                        AiApiLogScreen(onDismiss = { showAiLogScreen = false })
                    }
                }
            }
        }
    }
}

/** Drawer content: alarm controls and note editor visibility switches. */
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
    showFormatToolbar: Boolean,
    showAiChat: Boolean,
    onShowFormatToolbarChange: (Boolean) -> Unit,
    onShowAiChatChange: (Boolean) -> Unit,
    onShowAiLogs: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NoteEditorToggleItem(
            label = stringResource(R.string.note_editor_show_format_toolbar),
            checked = showFormatToolbar,
            onCheckedChange = onShowFormatToolbarChange,
        )
        NoteEditorToggleItem(
            label = stringResource(R.string.note_editor_show_ai_chat),
            checked = showAiChat,
            onCheckedChange = onShowAiChatChange,
        )

        DebugAiLogsSideMenuItem(onClick = onShowAiLogs)
    }
}

/** Label + switch row for a note editor preference. */
@Composable
private fun NoteEditorToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

/** Read-only dropdown for choosing reminder offset minutes. */
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
