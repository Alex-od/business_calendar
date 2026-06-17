package ua.danichapps.radiantdays.ui.addevent

import android.content.Context
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiAction
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.locale.AppLocaleStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.ui.common.ColoredTagChip
import ua.danichapps.radiantdays.ui.common.CompactFilterChip
import ua.danichapps.radiantdays.ui.common.TagChipSpacing
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import ua.danichapps.radiantdays.ui.common.NoteFormatToolbar
import ua.danichapps.radiantdays.ui.common.appendVoiceTextToRichFieldValue
import ua.danichapps.radiantdays.ui.common.applyBoldTyping
import ua.danichapps.radiantdays.ui.common.noteFieldValueToMarkdown
import ua.danichapps.radiantdays.ui.common.noteMarkdownToFieldValue
import ua.danichapps.radiantdays.ui.common.preserveSpansOnEdit
import ua.danichapps.radiantdays.ui.common.rememberVoiceInputLauncher
import ua.danichapps.radiantdays.ui.common.RichNoteTextField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Shared screen for adding a new event and editing an existing one.
 *
 * @param initialDayMillis Pre-sets start time when adding (ignored in edit mode).
 * @param editingEventId   Non-null -> edit mode; `null` -> add mode.
 * @param onNavigateBack   Pops the back stack when the user leaves the screen.
 * @param viewModel        Koin-provided; overridable for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    initialDayMillis: Long,
    editingEventId: Long?,
    onNavigateBack: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAiActions: () -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    createdTagGuid: String? = null,
    onCreatedTagGuidConsumed: () -> Unit = {},
    viewModel: AddEditEventViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editingEventId) {
        if (editingEventId != null) viewModel.loadEvent(editingEventId)
        else viewModel.setInitialDay(initialDayMillis)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEventUiEvent.NavigateBack -> onNavigateBack()
                is AddEditEventUiEvent.NavigateToAiChat -> onOpenAiChat()
                is AddEditEventUiEvent.ShowError    -> snackbarHostState.showSnackbar(event.message)
                is AddEditEventUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is AddEditEventUiEvent.NavigateBackFromAiChat -> Unit
            }
        }
    }

    LaunchedEffect(createdTagGuid) {
        val tagGuid = createdTagGuid ?: return@LaunchedEffect
        viewModel.onTagAddedFromSettings(tagGuid)
        onCreatedTagGuidConsumed()
    }

    BackHandler(onBack = viewModel::onBackClick)

    val context = LocalContext.current
    val voiceUnavailableMessage = stringResource(R.string.event_voice_unavailable)
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
            TagToolbar(
                uiState = uiState,
                onBackClick = viewModel::onBackClick,
                onTagToggle = viewModel::onTagToggle,
                onTagsExpandedToggle = viewModel::onTagsExpandedToggle,
                onOpenTags = onOpenTags,
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
            onDescriptionChangeFromVoice = viewModel::onDescriptionChangeFromVoice,
            onDescriptionUndo = viewModel::onDescriptionUndo,
            onAlarmTimeChange = viewModel::onAlarmTimeChange,
            onNotificationMinutesChange = viewModel::onNotificationMinutesChange,
            onIsCompletedChange = viewModel::onIsCompletedChange,
            onAddAlarm = { requestAlarmWithPermission() },
            onRemoveAlarm = viewModel::onRemoveAlarmClick,
            onAiClick = viewModel::onAiButtonClick,
            onVoiceInputUnavailable = {
                scope.launch {
                    snackbarHostState.showSnackbar(voiceUnavailableMessage)
                }
            },
            modifier = Modifier.padding(padding),
        )

        if (uiState.aiLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (uiState.aiSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::onAiSheetDismiss,
            sheetState = sheetState,
        ) {
            AiActionsBottomSheetContent(
                actions = uiState.visibleAiActions,
                onActionClick = viewModel::onAiActionSelected,
                onConfigureClick = {
                    viewModel.onAiSheetDismiss()
                    onOpenAiActions()
                },
            )
        }
    }

    uiState.aiResultText?.let { resultText ->
        AiResultDialog(
            resultText = resultText,
            onContinueChat = viewModel::onAiResultContinueChat,
            onReplace = viewModel::onAiResultReplace,
            onAppend = viewModel::onAiResultAppend,
            onDismiss = viewModel::onAiResultDismiss,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiResultDialog(
    resultText: String,
    onContinueChat: () -> Unit,
    onReplace: () -> Unit,
    onAppend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.event_ai_result_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(16.dp))
                val maxTextHeight = (LocalConfiguration.current.screenHeightDp * 0.50f).dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxTextHeight)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onContinueChat) {
                        Text(stringResource(R.string.event_continue_chat))
                    }
                    TextButton(onClick = onReplace) {
                        Text(stringResource(R.string.action_replace))
                    }
                    TextButton(onClick = onAppend) {
                        Text(stringResource(R.string.action_add))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventForm(
    uiState: AddEditEventUiState,
    onDescriptionChange: (String) -> Unit,
    onDescriptionChangeFromVoice: (String) -> Unit,
    onDescriptionUndo: () -> Unit,
    onAlarmTimeChange: (Long) -> Unit,
    onNotificationMinutesChange: (Int) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit,
    onAddAlarm: () -> Unit,
    onRemoveAlarm: () -> Unit,
    onAiClick: () -> Unit,
    onVoiceInputUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val localeStore: AppLocaleStore = koinInject()
    val locale = remember(context) { localeStore.resolveLocale(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("dd MMM yyyy", locale) }
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val typography = MaterialTheme.typography
    val bodyTextStyle = typography.bodyLarge
    val noteDisplayStyles = remember(typography) {
        NoteDisplayStyles(
            smallSize = typography.labelSmall.fontSize,
            normalSize = typography.bodyLarge.fontSize,
            largeSize = typography.headlineSmall.fontSize,
        )
    }

    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var showAlarmTimePicker by remember { mutableStateOf(false) }
    var boldTyping by remember { mutableStateOf(false) }
    var isDescriptionFocused by remember { mutableStateOf(false) }
    var forceExternalSync by remember { mutableIntStateOf(0) }
    var localMarkdown by remember(uiState.editingEventId) { mutableStateOf(uiState.description) }

    var descriptionValue by remember(uiState.editingEventId) {
        mutableStateOf(
            noteMarkdownToFieldValue(
                markdown = uiState.description,
                styles = noteDisplayStyles,
            ),
        )
    }

    LaunchedEffect(descriptionValue) {
        delay(400)
        val markdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
        if (markdown == localMarkdown) return@LaunchedEffect
        localMarkdown = markdown
        onDescriptionChange(markdown)
    }

    LaunchedEffect(uiState.description, forceExternalSync) {
        if (isDescriptionFocused && forceExternalSync == 0) return@LaunchedEffect
        if (uiState.description == localMarkdown) {
            forceExternalSync = 0
            return@LaunchedEffect
        }
        val fieldMarkdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
        if (uiState.description == fieldMarkdown) {
            localMarkdown = uiState.description
            forceExternalSync = 0
            return@LaunchedEffect
        }
        localMarkdown = uiState.description
        descriptionValue = noteMarkdownToFieldValue(
            markdown = uiState.description,
            styles = noteDisplayStyles,
            selection = descriptionValue.selection,
        )
        boldTyping = false
        forceExternalSync = 0
    }

    val voicePrompt = stringResource(R.string.event_voice_prompt)
    val startVoiceInput = rememberVoiceInputLauncher(
        locale = locale,
        prompt = voicePrompt,
        onResult = { spoken ->
            descriptionValue = appendVoiceTextToRichFieldValue(descriptionValue, spoken)
            val markdown = noteFieldValueToMarkdown(descriptionValue, noteDisplayStyles)
            localMarkdown = markdown
            onDescriptionChangeFromVoice(markdown)
        },
        onUnavailable = onVoiceInputUnavailable,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onDescriptionUndo()
                    forceExternalSync++
                },
                enabled = uiState.canUndoDescription,
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = startVoiceInput) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.event_voice_input))
            }
            if (uiState.alarmTimeMillis == null) {
                IconButton(onClick = onAddAlarm) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = stringResource(R.string.event_add_alarm))
                }
            } else {
                IconButton(onClick = onRemoveAlarm) {
                    Icon(Icons.Default.AlarmOff, contentDescription = stringResource(R.string.event_remove_alarm))
                }
            }
            IconButton(
                onClick = onAiClick,
                enabled = descriptionValue.text.isNotBlank(),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ai_chat_assistant))
            }
        }

        NoteFormatToolbar(
            value = descriptionValue,
            onValueChange = { descriptionValue = it },
            styles = noteDisplayStyles,
            boldTyping = boldTyping,
            onBoldTypingChange = { boldTyping = it },
        )

        RichNoteTextField(
            value = descriptionValue,
            onFocusChange = { isDescriptionFocused = it },
            onValueChange = { newValue ->
                val preserved = preserveSpansOnEdit(descriptionValue, newValue)
                val processed = if (boldTyping) {
                    applyBoldTyping(descriptionValue, preserved)
                } else {
                    preserved
                }
                descriptionValue = processed
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            textStyle = bodyTextStyle,
            minLines = 10,
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
                    onAlarmTimeChange(mergeTimeIntoMillis(alarmTimeMillis, state.hour, state.minute))
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
                Text(stringResource(R.string.event_reminder), style = MaterialTheme.typography.titleSmall)
                val pushDateStr = dateFormat.format(Date(fireMillis))
                val pushTimeStr = timeFormat.format(Date(fireMillis))
                Text(
                    text = stringResource(R.string.event_reminder_push, pushDateStr, pushTimeStr),
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
        Text(stringResource(R.string.event_quick_pick), style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(millisPlusMinutes(System.currentTimeMillis(), 15)) },
                    label = { Text(stringResource(R.string.event_preset_15_min)) },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(millisPlusHours(System.currentTimeMillis(), 1)) },
                    label = { Text(stringResource(R.string.event_preset_1_hour)) },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(tomorrowAtNineMillis()) },
                    label = { Text(stringResource(R.string.event_preset_tomorrow_9)) },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onPresetAlarm(noteDayAtNineMillis(startTimeMillis)) },
                    label = { Text(stringResource(R.string.event_preset_note_day_9)) },
                )
            }
        }
        Text(stringResource(R.string.event_minutes_before), style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(REMINDER_OFFSET_MINUTES_OPTIONS) { minutes ->
                FilterChip(
                    selected = notificationMinutesBefore == minutes,
                    onClick = { onNotificationMinutesChange(minutes) },
                    label = {
                        Text(
                            if (minutes == 0) {
                                stringResource(R.string.event_on_time)
                            } else {
                                stringResource(R.string.event_minutes_short, minutes)
                            },
                        )
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isCompleted, onCheckedChange = onIsCompletedChange)
            Text(stringResource(R.string.event_completed), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagToolbar(
    uiState: AddEditEventUiState,
    onBackClick: () -> Unit,
    onTagToggle: (String) -> Unit,
    onTagsExpandedToggle: () -> Unit,
    onOpenTags: () -> Unit,
) {
    Surface(
        color = TopAppBarDefaults.topAppBarColors().containerColor,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(vertical = TagChipSpacing.ToolbarVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            TagQuickAccessSection(
                modifier = Modifier.weight(1f),
                tags = uiState.tags,
                selectedTagGuids = uiState.selectedTagGuids,
                tagsExpanded = uiState.tagsExpanded,
                onTagToggle = onTagToggle,
                onTagsExpandedToggle = onTagsExpandedToggle,
                onOpenTags = onOpenTags,
            )
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
    modifier: Modifier = Modifier,
) {
    val selectedTags = tags.filter { it.guid in selectedTagGuids }
    val pinnedUnselectedTags = tags.filter { it.isPinned && it.guid !in selectedTagGuids }
    val otherUnselectedTags = tags.filter { !it.isPinned && it.guid !in selectedTagGuids }
    val hasMoreTags = otherUnselectedTags.isNotEmpty()
    val selectedListState = rememberLazyListState()
    val unselectedListState = rememberLazyListState()
    val tagGuids = tags.map { it.guid }

    LaunchedEffect(selectedTagGuids, tagGuids) {
        if (selectedTags.isNotEmpty()) {
            selectedListState.scrollToItem(0)
        }
        unselectedListState.scrollToItem(0)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTagRows),
    ) {
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                state = selectedListState,
                horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTags),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                items(
                    items = selectedTags,
                    key = { tag -> "selected_${tag.guid}" },
                ) { tag ->
                    ColoredTagChip(
                        name = if (tag.isUntaggedFilter) {
                            stringResource(R.string.tag_untagged)
                        } else {
                            tag.name
                        },
                        color = tag.color,
                        selected = true,
                        onClick = { onTagToggle(tag.guid) },
                    )
                }
            }
        }
        LazyRow(
            state = unselectedListState,
            horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenTags),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            items(
                items = pinnedUnselectedTags,
                key = { tag -> tag.guid },
            ) { tag ->
                ColoredTagChip(
                    name = if (tag.isUntaggedFilter) {
                        stringResource(R.string.tag_untagged)
                    } else {
                        tag.name
                    },
                    color = tag.color,
                    selected = false,
                    onClick = { onTagToggle(tag.guid) },
                )
            }
            if (tagsExpanded) {
                items(
                    items = otherUnselectedTags,
                    key = { tag -> tag.guid },
                ) { tag ->
                    ColoredTagChip(
                        name = if (tag.isUntaggedFilter) {
                            stringResource(R.string.tag_untagged)
                        } else {
                            tag.name
                        },
                        color = tag.color,
                        selected = false,
                        onClick = { onTagToggle(tag.guid) },
                    )
                }
            }
            item(key = "tag_actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(TagChipSpacing.BetweenActionButtons)) {
                    if (hasMoreTags) {
                        CompactFilterChip(
                            selected = tagsExpanded,
                            onClick = onTagsExpandedToggle,
                            label = { Text(stringResource(R.string.action_more)) },
                        )
                    }
                    CompactFilterChip(
                        selected = false,
                        onClick = onOpenTags,
                        contentPadding = TagChipSpacing.AddButtonContentPadding,
                        label = { Text("+") },
                    )
                }
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

@Composable
private fun AiActionsBottomSheetContent(
    actions: List<AiAction>,
    onActionClick: (String) -> Unit,
    onConfigureClick: () -> Unit,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.event_ai_actions),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (actions.isEmpty()) {
            Text(
                text = stringResource(R.string.event_no_actions),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onConfigureClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.action_configure))
            }
        } else {
            LazyColumn {
                items(actions, key = { it.guid }) { action ->
                    ListItem(
                        modifier = Modifier.clickable { onActionClick(action.guid) },
                        headlineContent = { Text(action.name) },
                        supportingContent = action.description?.let { description ->
                            {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

