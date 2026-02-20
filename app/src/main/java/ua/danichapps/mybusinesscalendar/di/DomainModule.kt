package ua.danichapps.mybusinesscalendar.di

import org.koin.dsl.module
import ua.danichapps.mybusinesscalendar.domain.usecase.AddEventUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.DeleteEventUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.GetEventsForDayUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.GetEventsForMonthUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.GetUpcomingEventsUseCase
import ua.danichapps.mybusinesscalendar.domain.usecase.UpdateEventUseCase

/**
 * Koin module for the domain layer.
 *
 * Use-cases are bound with `factory` (a new instance per injection site) because
 * they are stateless and lightweight — there is no benefit to keeping singletons.
 */
val domainModule = module {
    factory { GetEventsForDayUseCase(get()) }
    factory { GetEventsForMonthUseCase(get()) }
    factory { GetUpcomingEventsUseCase(get()) }
    factory { AddEventUseCase(get()) }
    factory { UpdateEventUseCase(get()) }
    factory { DeleteEventUseCase(get()) }
}
