package ua.danichapps.radiantdays.di

import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.ai.RadiantAiCompletionClientProvider
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.notification.EventNotificationManager
import ua.danichapps.radiantdays.sync.DeviceIdProvider
import ua.danichapps.radiantdays.sync.WebSocketBridgeClient
import ua.danichapps.radiantdays.ui.addevent.AddEditEventViewModel
import ua.danichapps.radiantdays.ui.aiactions.AiActionsViewModel
import ua.danichapps.radiantdays.ui.calendar.CalendarViewModel
import ua.danichapps.radiantdays.ui.settings.SettingsViewModel
import ua.danichapps.radiantdays.ui.tags.TagSettingsViewModel
import ua.danichapps.radiantdays.ui.tagnotes.TagNotesViewModel
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater

val presentationModule = module {

    viewModel {
        CalendarViewModel(
            getEventsForDayUseCase = get(),
            getEventsForMonthUseCase = get(),
            deleteEventUseCase = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
        )
    }

    viewModel {
        AddEditEventViewModel(
            addEventUseCase = get(),
            updateEventUseCase = get(),
            getTagsUseCase = get(),
            getVisibleAiActionsUseCase = get(),
            runAiActionUseCase = get(),
            repository = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
        )
    }

    viewModel {
        SettingsViewModel(apiKeyStore = get())
    }

    viewModel {
        AiActionsViewModel(
            getAiActionsUseCase = get(),
            addAiActionUseCase = get(),
            updateAiActionUseCase = get(),
            deleteAiActionUseCase = get(),
            reorderAiActionsUseCase = get(),
        )
    }

    viewModel {
        TagSettingsViewModel(
            getTagsUseCase = get(),
            addTagUseCase = get(),
            updateTagUseCase = get(),
            deleteTagUseCase = get(),
        )
    }

    viewModel { parameters ->
        TagNotesViewModel(
            tagGuid = parameters.get(),
            getEventsByTagUseCase = get(),
            getTagsUseCase = get(),
            deleteEventUseCase = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
        )
    }

    single { CalendarWidgetUpdater(get()) }
    single { EventNotificationManager(get()) }
    single { AlarmScheduler(get(), get()) }
    single { OkHttpClient() }
    single { AiApiKeyStore(get()) }
    single<AiCompletionClientProvider> {
        RadiantAiCompletionClientProvider(keyStore = get(), okHttpClient = get())
    }

    single { DeviceIdProvider(get()) }
    single {
        WebSocketBridgeClient(
            repository = get(),
            deviceIdProvider = get(),
        )
    }
}
