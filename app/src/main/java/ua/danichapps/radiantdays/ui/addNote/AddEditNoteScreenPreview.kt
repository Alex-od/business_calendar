package ua.danichapps.radiantdays.ui.addNote

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.module
import ua.danichapps.radiantdays.domain.model.EventColor
import ua.danichapps.radiantdays.domain.model.Tag
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

/** Preview: add-event screen with tags and description. */
@Preview(showBackground = true, name = "Add event", device = Devices.PIXEL_6)
@Composable
private fun AddEditNoteScreenPreview() {
    val context = LocalContext.current
    KoinApplication(application = {
        androidContext(context)
        modules(
            module {
                single { AppLocaleStore(get()) }
            },
        )
    }) {
        val localeStore: AppLocaleStore = koinInject()
        val locale = remember(context) { localeStore.resolveLocale(context) }
        RadiantDaysTheme(dynamicColor = false) {
            AddEditNoteScreenContent(
                uiState = AddEditNoteUiState(
                    description = "Team sync\n\nDiscuss Q3 priorities.",
                    tags = listOf(
                        Tag(guid = "work", name = "Work", color = EventColor.BLUE, isPinned = true),
                        Tag(guid = "personal", name = "Personal", color = EventColor.GREEN),
                    ),
                    selectedTagGuids = setOf("work"),
                ),
                callbacks = AddEditNoteScreenCallbacks(),
                snackbarHostState = remember { SnackbarHostState() },
                locale = locale,
            )
        }
    }
}

/** Preview: add-event screen in loading state. */
@Preview(showBackground = true, name = "Loading", device = Devices.PIXEL_6)
@Composable
private fun AddEditNoteScreenLoadingPreview() {
    val context = LocalContext.current
    KoinApplication(application = {
        androidContext(context)
        modules(
            module {
                single { AppLocaleStore(get()) }
            },
        )
    }) {
        val localeStore: AppLocaleStore = koinInject()
        val locale = remember(context) { localeStore.resolveLocale(context) }
        RadiantDaysTheme(dynamicColor = false) {
            AddEditNoteScreenContent(
                uiState = AddEditNoteUiState(isLoading = true),
                callbacks = AddEditNoteScreenCallbacks(),
                snackbarHostState = remember { SnackbarHostState() },
                locale = locale,
            )
        }
    }
}
