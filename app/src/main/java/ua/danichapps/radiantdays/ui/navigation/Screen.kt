package ua.danichapps.radiantdays.ui.navigation

/**
 * Sealed hierarchy that represents every navigation destination in the app.
 *
 * Using a sealed class instead of raw strings prevents typos and makes
 * navigation refactoring easy вЂ” the compiler catches all usages.
 */
sealed class Screen(val route: String) {

    /** Main calendar view with the month grid and event list. */
    data object Calendar : Screen("calendar")

    /** Empty settings placeholder. */
    data object Settings : Screen("settings")

    data object FolderSettings : Screen("folder_settings")

    /** Screen for creating a new event, pre-filled with [selectedDayMillis]. */
    data object AddEvent : Screen("add_event/{selectedDayMillis}") {
        fun createRoute(selectedDayMillis: Long) = "add_event/$selectedDayMillis"
        const val ARG_SELECTED_DAY = "selectedDayMillis"
    }

    /** Screen for editing an existing event identified by [eventId]. */
    data object EditEvent : Screen("edit_event/{eventId}") {
        fun createRoute(eventId: Long) = "edit_event/$eventId"
        const val ARG_EVENT_ID = "eventId"
    }
}
