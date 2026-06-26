package ua.danichapps.radiantdays.ui.navigation

import androidx.navigation.NavHostController

/**
 * Navigate without stacking duplicate destinations on rapid re-entry.
 * See compose-kotlin-agent-skills references/07-navigation.md — launchSingleTop.
 */
internal fun NavHostController.navigateForward(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * Opens an auxiliary screen (settings, tags, AI actions).
 * If the same screen is already on top, pop it first to avoid stale duplicates.
 */
internal fun NavHostController.navigateAuxiliary(route: String, routePrefix: String) {
    val currentRoute = currentDestination?.route
    if (currentRoute != null && currentRoute.startsWith(routePrefix)) {
        popBackStack()
    }
    navigateForward(route)
}

internal fun NavHostController.navigateToAddEvent(dayMillis: Long) {
    navigateForward(Screen.AddEvent.createRoute(dayMillis))
}

/**
 * Opens the note editor.
 * - From calendar / notification: plain forward navigation.
 * - From tag notes: pop editors above the list so back returns to the same list.
 */
internal fun NavHostController.navigateToEditEvent(eventId: Long) {
    val route = Screen.EditEvent.createRoute(eventId)
    val tagNotesInStack = runCatching {
        getBackStackEntry(Screen.TagNotes.route)
    }.isSuccess

    if (tagNotesInStack) {
        navigate(route) {
            launchSingleTop = true
            popUpTo(Screen.TagNotes.route) { inclusive = false }
        }
        return
    }

    if (currentDestination?.route?.startsWith("edit_event") == true) {
        popBackStack()
    }
    navigateForward(route)
}

internal fun NavHostController.navigateToTagSettings(returnAfterCreate: Boolean = false) {
    navigateAuxiliary(
        route = Screen.TagSettings.createRoute(returnAfterCreate),
        routePrefix = "tag_settings",
    )
}

internal fun NavHostController.navigateToTagNotes(tagGuid: String) {
    navigateForward(Screen.TagNotes.createRoute(tagGuid))
}
