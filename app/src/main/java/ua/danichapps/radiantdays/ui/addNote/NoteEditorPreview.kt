package ua.danichapps.radiantdays.ui.addNote

import android.content.res.Configuration
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
import java.util.Locale

private const val PREVIEW_DESCRIPTION = "Team sync\n\nDiscuss Q3 priorities."

/** Preview: note editor without AI chat. */
@Preview(showBackground = true, name = "Note editor", device = Devices.PIXEL_6)
@Preview(
    showBackground = true,
    name = "Note editor (night)",
    device = Devices.PIXEL_6,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NoteEditorPreview() {
    NoteEditorPreviewContent(
        uiState = AddEditNoteUiState(
            description = PREVIEW_DESCRIPTION,
            canUndoDescription = true,
            isAiKeySaved = true,
        ),
    )
}

/** Preview: note editor with visible AI chat messages. */
@Preview(showBackground = true, name = "Note editor with AI chat", device = Devices.PIXEL_6, heightDp = 640)
@Composable
private fun NoteEditorWithAiChatPreview() {
    NoteEditorPreviewContent(
        uiState = AddEditNoteUiState(
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
private fun NoteEditorPreviewContent(uiState: AddEditNoteUiState) {
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
            val localeStore = org.koin.compose.koinInject<AppLocaleStore>()
            val locale = remember(context) { localeStore.resolveLocale(context) }
            val noteEditorState = rememberNoteEditorState(
                description = uiState.description,
                editingNoteId = uiState.editingNoteId,
                locale = locale,
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
                NoteEditor(
                    state = noteEditorState,
                    uiState = uiState,
                    callbacks = AddEditNoteScreenCallbacks(),
                    noteDisplayStyles = noteDisplayStyles,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
