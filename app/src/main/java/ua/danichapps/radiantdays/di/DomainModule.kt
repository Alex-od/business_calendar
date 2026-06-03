package ua.danichapps.radiantdays.di

import org.koin.dsl.module
import ua.danichapps.radiantdays.domain.usecase.AddFolderUseCase
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteEventUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteFolderUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsByFolderUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForDayUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForMonthUseCase
import ua.danichapps.radiantdays.domain.usecase.GetPendingRemindersUseCase
import ua.danichapps.radiantdays.domain.usecase.GetUpcomingEventsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetFoldersUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateFolderUseCase

/**
 * Koin module for the domain layer.
 *
 * Use-cases are bound with `factory` (a new instance per injection site) because
 * they are stateless and lightweight вЂ” there is no benefit to keeping singletons.
 */
val domainModule = module {
    factory { GetEventsForDayUseCase(get()) }
    factory { GetEventsByFolderUseCase(get()) }
    factory { GetEventsForMonthUseCase(get()) }
    factory { GetUpcomingEventsUseCase(get()) }
    factory { GetPendingRemindersUseCase(get()) }
    factory { AddEventUseCase(get()) }
    factory { UpdateEventUseCase(get()) }
    factory { DeleteEventUseCase(get()) }
    factory { GetFoldersUseCase(get()) }
    factory { AddFolderUseCase(get()) }
    factory { UpdateFolderUseCase(get()) }
    factory { DeleteFolderUseCase(get()) }
}
