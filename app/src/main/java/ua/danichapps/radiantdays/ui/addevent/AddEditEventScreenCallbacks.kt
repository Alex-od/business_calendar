package ua.danichapps.radiantdays.ui.addevent

internal data class AddEditEventScreenCallbacks(
    val onBackClick: () -> Unit = {},
    val onTagToggle: (String) -> Unit = {},
    val onTagsExpandedToggle: () -> Unit = {},
    val onAiChatSend: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onDescriptionChangeFromVoice: (String) -> Unit = {},
    val onDescriptionUndo: () -> Unit = {},
    val onAlarmTimeChange: (Long) -> Unit = {},
    val onNotificationMinutesChange: (Int) -> Unit = {},
    val onAddAlarm: () -> Unit = {},
    val onRemoveAlarm: () -> Unit = {},
    val onAiClick: () -> Unit = {},
    val onAiChatMessageEdit: (Int, String) -> Unit = { _, _ -> },
    val onAiChatMessageDelete: (Int) -> Unit = {},
    val onAiChatMessageCopied: () -> Unit = {},
    val onVoiceInputUnavailable: () -> Unit = {},
    val onShowFormatToolbarChange: (Boolean) -> Unit = {},
    val onShowAiChatChange: (Boolean) -> Unit = {},
    val onAiSheetDismiss: () -> Unit = {},
    val onAiActionSelected: (String) -> Unit = {},
)
