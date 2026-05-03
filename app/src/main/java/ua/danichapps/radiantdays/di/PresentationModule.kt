package ua.danichapps.radiantdays.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.danichapps.radiantdays.notification.EventNotificationManager
import ua.danichapps.radiantdays.sync.DeviceIdProvider
import ua.danichapps.radiantdays.sync.WebSocketBridgeClient
import ua.danichapps.radiantdays.ui.addevent.AddEditEventViewModel
import ua.danichapps.radiantdays.ui.calendar.CalendarViewModel
import ua.danichapps.radiantdays.ui.folders.FolderSettingsViewModel

/**
 * Koin module for the presentation layer.
 *
 * ViewModels are registered with `viewModel { }` so Koin integrates with
 * the Android ViewModel lifecycle (survives configuration changes).
 */
val presentationModule = module {

    // ViewModels
    viewModel {
        CalendarViewModel(
            getEventsForDayUseCase = get(),
            getEventsForMonthUseCase = get(),
            deleteEventUseCase = get(),
        )
    }

    viewModel {
        AddEditEventViewModel(
            addEventUseCase = get(),
            updateEventUseCase = get(),
            getFoldersUseCase = get(),
            repository = get(),
        )
    }

    viewModel {
        FolderSettingsViewModel(
            getFoldersUseCase = get(),
            addFolderUseCase = get(),
            updateFolderUseCase = get(),
            deleteFolderUseCase = get(),
        )
    }

    // Notification manager (used by Worker via KoinComponent)
    single { EventNotificationManager(get()) }

    // WebSocket bridge (dev-only local sync channel)
    single { DeviceIdProvider(get()) }
    single {
        WebSocketBridgeClient(
            repository = get(),
            deviceIdProvider = get(),
        )
    }
}
