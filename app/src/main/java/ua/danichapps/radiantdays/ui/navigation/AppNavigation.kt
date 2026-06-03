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
import ua.danichapps.radiantdays.ui.folders.FolderSettingsScreen
import ua.danichapps.radiantdays.ui.foldernotes.FolderNotesScreen
import ua.danichapps.radiantdays.ui.settings.SettingsScreen

/**
 * Root navigation graph.
 *
 * Compose Navigation is configured here; screens are composable lambdas,
 * keeping the NavHost completely declarative.
 */
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
        navController  = navController,
        startDestination = Screen.Calendar.route,
    ) {

        // в”Ђв”Ђ Calendar в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddEvent  = { dayMillis ->
                    navController.navigate(Screen.AddEvent.createRoute(dayMillis))
                },
                onEditEvent = { eventId ->
                    navController.navigate(Screen.EditEvent.createRoute(eventId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenFolders = {
                    navController.navigate(Screen.FolderSettings.createRoute())
                },
            )
        }

        // в”Ђв”Ђ Add new event в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.FolderSettings.route,
            arguments = listOf(
                navArgument(Screen.FolderSettings.ARG_RETURN_AFTER_CREATE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStack ->
            val returnAfterCreate = backStack.arguments
                ?.getBoolean(Screen.FolderSettings.ARG_RETURN_AFTER_CREATE)
                ?: false

            FolderSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                returnAfterCreate = returnAfterCreate,
                onFolderCreated = { folderGuid ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        Screen.FolderSettings.RESULT_CREATED_FOLDER_GUID,
                        folderGuid,
                    )
                },
                onOpenFolder = { folderGuid ->
                    navController.navigate(Screen.FolderNotes.createRoute(folderGuid))
                },
            )
        }

        composable(
            route = Screen.FolderNotes.route,
            arguments = listOf(
                navArgument(Screen.FolderNotes.ARG_FOLDER_GUID) { type = NavType.StringType },
            ),
        ) { backStack ->
            val folderGuid = backStack.arguments?.getString(Screen.FolderNotes.ARG_FOLDER_GUID)
                ?: return@composable

            FolderNotesScreen(
                folderGuid = folderGuid,
                onNavigateBack = { navController.popBackStack() },
                onEditNote = { noteId ->
                    navController.navigate(Screen.EditEvent.createRoute(noteId))
                },
            )
        }

        composable(
            route     = Screen.AddEvent.route,
            arguments = listOf(
                navArgument(Screen.AddEvent.ARG_SELECTED_DAY) { type = NavType.LongType },
            ),
        ) { backStack ->
            val selectedDay = backStack.arguments?.getLong(Screen.AddEvent.ARG_SELECTED_DAY)
                ?: System.currentTimeMillis()
            val createdFolderGuid = backStack.savedStateHandle
                .getStateFlow<String?>(Screen.FolderSettings.RESULT_CREATED_FOLDER_GUID, null)
                .collectAsStateWithLifecycle()

            AddEditEventScreen(
                initialDayMillis = selectedDay,
                editingEventId   = null,
                onNavigateBack   = { navController.popBackStack() },
                onOpenFolders    = { navController.navigate(Screen.FolderSettings.createRoute(returnAfterCreate = true)) },
                createdFolderGuid = createdFolderGuid.value,
                onCreatedFolderGuidConsumed = {
                    backStack.savedStateHandle[Screen.FolderSettings.RESULT_CREATED_FOLDER_GUID] = null
                },
            )
        }

        // в”Ђв”Ђ Edit existing event в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
        composable(
            route     = Screen.EditEvent.route,
            arguments = listOf(
                navArgument(Screen.EditEvent.ARG_EVENT_ID) { type = NavType.LongType },
            ),
        ) { backStack ->
            val eventId = backStack.arguments?.getLong(Screen.EditEvent.ARG_EVENT_ID)
                ?: return@composable
            val createdFolderGuid = backStack.savedStateHandle
                .getStateFlow<String?>(Screen.FolderSettings.RESULT_CREATED_FOLDER_GUID, null)
                .collectAsStateWithLifecycle()

            AddEditEventScreen(
                initialDayMillis = System.currentTimeMillis(),
                editingEventId   = eventId,
                onNavigateBack   = { navController.popBackStack() },
                onOpenFolders    = { navController.navigate(Screen.FolderSettings.createRoute(returnAfterCreate = true)) },
                createdFolderGuid = createdFolderGuid.value,
                onCreatedFolderGuidConsumed = {
                    backStack.savedStateHandle[Screen.FolderSettings.RESULT_CREATED_FOLDER_GUID] = null
                },
            )
        }
    }
}
