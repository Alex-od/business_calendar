package ua.danichapps.radiantdays.di

import org.koin.dsl.module
import ua.danichapps.radiantdays.domain.usecase.AddAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.AddTagUseCase
import ua.danichapps.radiantdays.domain.usecase.AddEventUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteEventUseCase
import ua.danichapps.radiantdays.domain.usecase.DeleteTagUseCase
import ua.danichapps.radiantdays.domain.usecase.GetAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsByTagUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForDayUseCase
import ua.danichapps.radiantdays.domain.usecase.GetEventsForMonthUseCase
import ua.danichapps.radiantdays.domain.usecase.GetPendingRemindersUseCase
import ua.danichapps.radiantdays.domain.usecase.GetUpcomingEventsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetTagsUseCase
import ua.danichapps.radiantdays.domain.usecase.GetVisibleAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.ReorderAiActionsUseCase
import ua.danichapps.radiantdays.domain.usecase.ContinueAiChatUseCase
import ua.danichapps.radiantdays.domain.usecase.RunAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateAiActionUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateEventUseCase
import ua.danichapps.radiantdays.domain.usecase.UpdateTagUseCase

val domainModule = module {
    factory { GetEventsForDayUseCase(get()) }
    factory { GetEventsByTagUseCase(get()) }
    factory { GetEventsForMonthUseCase(get()) }
    factory { GetUpcomingEventsUseCase(get()) }
    factory { GetPendingRemindersUseCase(get()) }
    factory { AddEventUseCase(get()) }
    factory { UpdateEventUseCase(get()) }
    factory { DeleteEventUseCase(get()) }
    factory { GetTagsUseCase(get()) }
    factory { AddTagUseCase(get()) }
    factory { UpdateTagUseCase(get()) }
    factory { DeleteTagUseCase(get()) }
    factory { GetAiActionsUseCase(get()) }
    factory { GetVisibleAiActionsUseCase(get()) }
    factory { AddAiActionUseCase(get()) }
    factory { UpdateAiActionUseCase(get()) }
    factory { DeleteAiActionUseCase(get()) }
    factory { ReorderAiActionsUseCase(get()) }
    factory { RunAiActionUseCase(get(), get()) }
    factory { ContinueAiChatUseCase(get()) }
}
