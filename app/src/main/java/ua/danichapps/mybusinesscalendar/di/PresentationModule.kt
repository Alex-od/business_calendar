package ua.danichapps.mybusinesscalendar.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.danichapps.mybusinesscalendar.notification.EventNotificationManager
import ua.danichapps.mybusinesscalendar.ui.addevent.AddEditEventViewModel
import ua.danichapps.mybusinesscalendar.ui.calendar.CalendarViewModel

/**
 * Koin module for the presentation layer.
 *
 * ViewModels are registered with `viewModel { }` so Koin integrates with
 * the Android ViewModel lifecycle (survives configuration changes).
 */
val presentationModule = module {

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel {
        CalendarViewModel(
            getEventsForDayUseCase   = get(),
            getEventsForMonthUseCase = get(),
            deleteEventUseCase       = get(),
        )
    }

    viewModel {
        AddEditEventViewModel(
            addEventUseCase    = get(),
            updateEventUseCase = get(),
            repository         = get(),
        )
    }

    // ── Notification manager (needed by the Worker via KoinComponent) ─────────
    single { EventNotificationManager(get()) }
}
