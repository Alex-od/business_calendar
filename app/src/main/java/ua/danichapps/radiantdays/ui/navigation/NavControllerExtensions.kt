package ua.danichapps.radiantdays.ui.navigation

import androidx.navigation.NavHostController

/** Navigate without stacking duplicate destinations on rapid re-entry. */
internal fun NavHostController.navigateForward(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

internal fun NavHostController.navigateToAddEvent(dayMillis: Long) {
    navigateForward(Screen.AddEvent.createRoute(dayMillis))
}

/**
 * Opens the note editor. When launched from [Screen.TagNotes], replaces any editor
 * already stacked above the tag-notes list instead of pushing another copy.
 */
internal fun NavHostController.navigateToEditEvent(eventId: Long) {
    navigate(Screen.EditEvent.createRoute(eventId)) {
        launchSingleTop = true
        popUpTo(Screen.TagNotes.route) { inclusive = false }
    }
}

internal fun NavHostController.navigateToTagSettings(returnAfterCreate: Boolean = false) {
    navigateForward(Screen.TagSettings.createRoute(returnAfterCreate))
}

internal fun NavHostController.navigateToTagNotes(tagGuid: String) {
    navigateForward(Screen.TagNotes.createRoute(tagGuid))
}
