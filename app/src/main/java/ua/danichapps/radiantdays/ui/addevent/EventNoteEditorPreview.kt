package ua.danichapps.radiantdays.ui.addevent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import ua.danichapps.radiantdays.domain.model.AiChatMessage
import ua.danichapps.radiantdays.domain.model.AiChatRole
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.ui.common.NoteDisplayStyles
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

private const val PREVIEW_DESCRIPTION = "Team sync\n\nDiscuss Q3 priorities."

/** Preview: note editor without AI chat. */
@Preview(showBackground = true, name = "Note editor", device = Devices.PIXEL_6)
@Composable
private fun EventNoteEditorPreview() {
    EventNoteEditorPreviewContent(
        uiState = AddEditEventUiState(
            description = PREVIEW_DESCRIPTION,
            canUndoDescription = true,
            isAiKeySaved = true,
        ),
    )
}

/** Preview: note editor with visible AI chat messages. */
@Preview(showBackground = true, name = "Note editor with AI chat", device = Devices.PIXEL_6, heightDp = 640)
@Composable
private fun EventNoteEditorWithAiChatPreview() {
    EventNoteEditorPreviewContent(
        uiState = AddEditEventUiState(
            description = PREVIEW_DESCRIPTION,
            canUndoDescription = true,
            isAiKeySaved = true,
            showAiChat = true,
            aiChatMessages = listOf(
                AiChatMessage(AiChatRole.USER, PREVIEW_DESCRIPTION),
                AiChatMessage(
                    AiChatRole.ASSISTANT,
                    "Consider adding agenda items for budget review.",
                ),
            ),
        ),
    )
}

/** Shared preview wrapper with Koin and note editor state. */
@Composable
private fun EventNoteEditorPreviewContent(uiState: AddEditEventUiState) {
    val context = LocalContext.current
    KoinApplication(application = {
        androidContext(context)
        modules(
            module {
                single { AppLocaleStore(get()) }
            },
        )
    }) {
        RadiantDaysTheme(dynamicColor = false) {
            val typography = MaterialTheme.typography
            val noteDisplayStyles = remember(typography) {
                NoteDisplayStyles(
                    smallSize = typography.labelSmall.fontSize,
                    normalSize = typography.bodyLarge.fontSize,
                    largeSize = typography.headlineSmall.fontSize,
                )
            }
            val noteEditorState = rememberEventNoteEditorState(
                description = uiState.description,
                editingEventId = uiState.editingEventId,
                noteDisplayStyles = noteDisplayStyles,
                onDescriptionChange = {},
                onDescriptionChangeFromVoice = {},
                onDescriptionUndo = {},
                onVoiceInputUnavailable = {},
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
            ) {
                EventNoteEditor(
                    state = noteEditorState,
                    uiState = uiState,
                    callbacks = AddEditEventScreenCallbacks(),
                    noteDisplayStyles = noteDisplayStyles,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
