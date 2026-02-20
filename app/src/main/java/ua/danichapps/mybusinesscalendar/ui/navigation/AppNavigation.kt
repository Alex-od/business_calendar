package ua.danichapps.mybusinesscalendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.danichapps.mybusinesscalendar.ui.addevent.AddEditEventScreen
import ua.danichapps.mybusinesscalendar.ui.calendar.CalendarScreen

/**
 * Root navigation graph.
 *
 * Compose Navigation is configured here; screens are composable lambdas,
 * keeping the NavHost completely declarative.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController  = navController,
        startDestination = Screen.Calendar.route,
    ) {

        // ── Calendar ──────────────────────────────────────────────────────────
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddEvent  = { dayMillis ->
                    navController.navigate(Screen.AddEvent.createRoute(dayMillis))
                },
                onEditEvent = { eventId ->
                    navController.navigate(Screen.EditEvent.createRoute(eventId))
                },
            )
        }

        // ── Add new event ─────────────────────────────────────────────────────
        composable(
            route     = Screen.AddEvent.route,
            arguments = listOf(
                navArgument(Screen.AddEvent.ARG_SELECTED_DAY) { type = NavType.LongType },
            ),
        ) { backStack ->
            val selectedDay = backStack.arguments?.getLong(Screen.AddEvent.ARG_SELECTED_DAY)
                ?: System.currentTimeMillis()

            AddEditEventScreen(
                initialDayMillis = selectedDay,
                editingEventId   = null,
                onNavigateBack   = { navController.popBackStack() },
            )
        }

        // ── Edit existing event ───────────────────────────────────────────────
        composable(
            route     = Screen.EditEvent.route,
            arguments = listOf(
                navArgument(Screen.EditEvent.ARG_EVENT_ID) { type = NavType.LongType },
            ),
        ) { backStack ->
            val eventId = backStack.arguments?.getLong(Screen.EditEvent.ARG_EVENT_ID)
                ?: return@composable

            AddEditEventScreen(
                initialDayMillis = System.currentTimeMillis(),
                editingEventId   = eventId,
                onNavigateBack   = { navController.popBackStack() },
            )
        }
    }
}
