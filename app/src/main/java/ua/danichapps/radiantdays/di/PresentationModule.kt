package ua.danichapps.radiantdays.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.notification.EventNotificationManager
import ua.danichapps.radiantdays.sync.DeviceIdProvider
import ua.danichapps.radiantdays.sync.WebSocketBridgeClient
import ua.danichapps.radiantdays.ui.addevent.AddEditEventViewModel
import ua.danichapps.radiantdays.ui.calendar.CalendarViewModel
import ua.danichapps.radiantdays.ui.folders.FolderSettingsViewModel
import ua.danichapps.radiantdays.ui.foldernotes.FolderNotesViewModel

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
            alarmScheduler = get(),
        )
    }

    viewModel {
        AddEditEventViewModel(
            addEventUseCase = get(),
            updateEventUseCase = get(),
            getFoldersUseCase = get(),
            repository = get(),
            alarmScheduler = get(),
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

    viewModel { parameters ->
        FolderNotesViewModel(
            folderGuid = parameters.get(),
            getEventsByFolderUseCase = get(),
            getFoldersUseCase = get(),
            deleteEventUseCase = get(),
            alarmScheduler = get(),
        )
    }

    single { EventNotificationManager(get()) }
    single { AlarmScheduler(get(), get()) }

    // WebSocket bridge (dev-only local sync channel)
    single { DeviceIdProvider(get()) }
    single {
        WebSocketBridgeClient(
            repository = get(),
            deviceIdProvider = get(),
        )
    }
}
