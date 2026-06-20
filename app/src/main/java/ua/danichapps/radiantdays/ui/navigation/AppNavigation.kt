package ua.danichapps.radiantdays.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.danichapps.radiantdays.ui.addevent.AddEditEventScreen
import ua.danichapps.radiantdays.ui.calendar.CalendarScreen
import ua.danichapps.radiantdays.ui.aiactions.AiActionsScreen
import ua.danichapps.radiantdays.ui.settings.AiSettingsScreen
import ua.danichapps.radiantdays.ui.tags.TagSettingsScreen
import ua.danichapps.radiantdays.ui.tagnotes.TagNotesScreen

@Composable
fun AppNavigation(
    pendingEditEventId: Long? = null,
    onPendingEditEventConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingEditEventId) {
        val eventId = pendingEditEventId ?: return@LaunchedEffect
        navController.navigate(Screen.EditEvent.createRoute(eventId)) {
            launchSingleTop = true
        }
        onPendingEditEventConsumed()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route,
    ) {

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddEvent = { dayMillis ->
                    navController.navigate(Screen.AddEvent.createRoute(dayMillis))
                },
                onEditEvent = { eventId ->
                    navController.navigate(Screen.EditEvent.createRoute(eventId))
                },
                onOpenAiSettings = { navController.navigate(Screen.AiSettings.route) },
                onOpenTags = { navController.navigate(Screen.TagSettings.createRoute()) },
            )
        }

        composable(Screen.AiSettings.route) {
            AiSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenAiActions = { navController.navigate(Screen.AiActions.route) },
            )
        }

        composable(Screen.AiActions.route) {
            AiActionsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.TagSettings.route,
            arguments = listOf(
                navArgument(Screen.TagSettings.ARG_RETURN_AFTER_CREATE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStack ->
            val returnAfterCreate = backStack.arguments
                ?.getBoolean(Screen.TagSettings.ARG_RETURN_AFTER_CREATE)
                ?: false

            TagSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                returnAfterCreate = returnAfterCreate,
                onTagCreated = { tagGuid ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        Screen.TagSettings.RESULT_CREATED_TAG_GUID,
                        tagGuid,
                    )
                },
                onOpenTag = { tagGuid ->
                    navController.navigate(Screen.TagNotes.createRoute(tagGuid))
                },
            )
        }

        composable(
            route = Screen.TagNotes.route,
            arguments = listOf(
                navArgument(Screen.TagNotes.ARG_TAG_GUID) { type = NavType.StringType },
            ),
        ) { backStack ->
            val tagGuid = backStack.arguments?.getString(Screen.TagNotes.ARG_TAG_GUID)
                ?: return@composable

            TagNotesScreen(
                tagGuid = tagGuid,
                onNavigateBack = { navController.popBackStack() },
                onEditNote = { noteId ->
                    navController.navigate(Screen.EditEvent.createRoute(noteId))
                },
            )
        }

        composable(
            route = Screen.AddEvent.route,
            arguments = listOf(
                navArgument(Screen.AddEvent.ARG_SELECTED_DAY) { type = NavType.LongType },
            ),
        ) { backStack ->
            val selectedDay = backStack.arguments?.getLong(Screen.AddEvent.ARG_SELECTED_DAY)
                ?: System.currentTimeMillis()
            val createdTagGuid = backStack.savedStateHandle
                .getStateFlow<String?>(Screen.TagSettings.RESULT_CREATED_TAG_GUID, null)
                .collectAsStateWithLifecycle()

            AddEditEventScreen(
                initialDayMillis = selectedDay,
                editingEventId = null,
                onNavigateBack = { navController.popBackStack() },
                onOpenTags = { navController.navigate(Screen.TagSettings.createRoute(returnAfterCreate = true)) },
                onOpenAiActions = { navController.navigate(Screen.AiActions.route) },
                createdTagGuid = createdTagGuid.value,
                onCreatedTagGuidConsumed = {
                    backStack.savedStateHandle[Screen.TagSettings.RESULT_CREATED_TAG_GUID] = null
                },
            )
        }

        composable(
            route = Screen.EditEvent.route,
            arguments = listOf(
                navArgument(Screen.EditEvent.ARG_EVENT_ID) { type = NavType.LongType },
            ),
        ) { backStack ->
            val eventId = backStack.arguments?.getLong(Screen.EditEvent.ARG_EVENT_ID)
                ?: return@composable
            val createdTagGuid = backStack.savedStateHandle
                .getStateFlow<String?>(Screen.TagSettings.RESULT_CREATED_TAG_GUID, null)
                .collectAsStateWithLifecycle()

            AddEditEventScreen(
                initialDayMillis = System.currentTimeMillis(),
                editingEventId = eventId,
                onNavigateBack = { navController.popBackStack() },
                onOpenTags = { navController.navigate(Screen.TagSettings.createRoute(returnAfterCreate = true)) },
                onOpenAiActions = { navController.navigate(Screen.AiActions.route) },
                createdTagGuid = createdTagGuid.value,
                onCreatedTagGuidConsumed = {
                    backStack.savedStateHandle[Screen.TagSettings.RESULT_CREATED_TAG_GUID] = null
                },
            )
        }
    }
}
