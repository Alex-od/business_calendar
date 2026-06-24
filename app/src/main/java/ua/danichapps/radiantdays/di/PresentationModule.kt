package ua.danichapps.radiantdays.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.danichapps.radiantdays.ai.AiApiKeyStore
import ua.danichapps.radiantdays.ai.AiApiRequestLogSink
import ua.danichapps.radiantdays.ai.AiApiRequestLogStore
import ua.danichapps.radiantdays.ai.OpenAiCompletionClientFactory
import ua.danichapps.radiantdays.ai.RadiantAiCompletionClientProvider
import ua.danichapps.radiantdays.ai.createAiOkHttpClient
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientFactory
import ua.danichapps.radiantdays.domain.repository.AiCompletionClientProvider
import ua.danichapps.radiantdays.locale.AppLocaleManager
import ua.danichapps.radiantdays.locale.AppLocaleStore
import ua.danichapps.radiantdays.locale.AppStrings
import ua.danichapps.radiantdays.locale.DomainErrorStrings
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.notification.EventNotificationManager
import ua.danichapps.radiantdays.sync.DeviceIdProvider
import ua.danichapps.radiantdays.sync.WebSocketBridgeClient
import ua.danichapps.radiantdays.ui.addNote.AddEditNoteViewModel
import ua.danichapps.radiantdays.ui.addNote.NoteEditorPreferencesStore
import ua.danichapps.radiantdays.ui.theme.AppThemeStore
import ua.danichapps.radiantdays.ui.aiactions.AiActionsViewModel
import ua.danichapps.radiantdays.ui.calendar.CalendarViewModel
import ua.danichapps.radiantdays.ui.settings.AiSettingsViewModel
import ua.danichapps.radiantdays.ui.settings.SettingsViewModel
import ua.danichapps.radiantdays.ui.tags.TagSettingsViewModel
import ua.danichapps.radiantdays.ui.tagnotes.TagNotesViewModel
import ua.danichapps.radiantdays.widget.CalendarWidgetUpdater

val presentationModule = module {

    viewModel {
        CalendarViewModel(
            getEventsForDayUseCase = get(),
            getEventsForMonthUseCase = get(),
            getTagsUseCase = get(),
            deleteEventUseCase = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            errorStrings = get(),
        )
    }

    viewModel {
        AddEditNoteViewModel(
            addEventUseCase = get(),
            updateEventUseCase = get(),
            getTagsUseCase = get(),
            getVisibleAiActionsUseCase = get(),
            runAiActionUseCase = get(),
            continueAiChatUseCase = get(),
            getEventByIdUseCase = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            localeStore = get(),
            apiKeyStore = get(),
            noteEditorPreferencesStore = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            apiKeyStore = get(),
            localeStore = get(),
            localeManager = get(),
            themeStore = get(),
        )
    }

    viewModel {
        AiSettingsViewModel(
            apiKeyStore = get(),
            appStrings = get(),
            validateAiApiKeyUseCase = get(),
            errorStrings = get(),
        )
    }

    viewModel {
        AiActionsViewModel(
            getAiActionsUseCase = get(),
            addAiActionUseCase = get(),
            updateAiActionUseCase = get(),
            deleteAiActionUseCase = get(),
            reorderAiActionsUseCase = get(),
            errorStrings = get(),
        )
    }

    viewModel {
        TagSettingsViewModel(
            getTagsUseCase = get(),
            addTagUseCase = get(),
            updateTagUseCase = get(),
            deleteTagUseCase = get(),
            errorStrings = get(),
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
            errorStrings = get(),
        )
    }

    single { CalendarWidgetUpdater(get()) }
    single { EventNotificationManager(get()) }
    single { AlarmScheduler(get(), get()) }
    single { createAiOkHttpClient() }
    single<AiApiRequestLogSink> { AiApiRequestLogStore(get()) }
    single { AiApiKeyStore(get()) }
    single { AppLocaleStore(get()) }
    single { AppThemeStore(get()) }
    single { NoteEditorPreferencesStore(get()) }
    single { AppLocaleManager() }
    single { DomainErrorStrings(get()) }
    single { AppStrings(get()) }
    single {
        OpenAiCompletionClientFactory(okHttpClient = get(), logSink = get())
    }
    single<AiCompletionClientFactory> { get<OpenAiCompletionClientFactory>() }
    single<AiCompletionClientProvider> {
        RadiantAiCompletionClientProvider(
            keyStore = get(),
            clientFactory = get(),
            appStrings = get(),
        )
    }

    single { DeviceIdProvider(get()) }
    single {
        WebSocketBridgeClient(
            repository = get(),
            deviceIdProvider = get(),
        )
    }
}
