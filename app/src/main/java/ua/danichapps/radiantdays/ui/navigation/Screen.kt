package ua.danichapps.radiantdays.ui.navigation

sealed class Screen(val route: String) {

    data object Calendar : Screen("calendar")

    data object Settings : Screen("settings")

    data object AiActions : Screen("ai_actions")

    data object TagSettings : Screen("tag_settings?returnAfterCreate={returnAfterCreate}") {
        const val ARG_RETURN_AFTER_CREATE = "returnAfterCreate"
        const val RESULT_CREATED_TAG_GUID = "createdTagGuid"

        fun createRoute(returnAfterCreate: Boolean = false) =
            "tag_settings?returnAfterCreate=$returnAfterCreate"
    }

    data object AddEvent : Screen("add_event/{selectedDayMillis}") {
        fun createRoute(selectedDayMillis: Long) = "add_event/$selectedDayMillis"
        const val ARG_SELECTED_DAY = "selectedDayMillis"
    }

    data object EditEvent : Screen("edit_event/{eventId}") {
        fun createRoute(eventId: Long) = "edit_event/$eventId"
        const val ARG_EVENT_ID = "eventId"
    }

    /** Notes filtered by tag (or virtual «Без тегов»). */
    data object TagNotes : Screen("tag_notes/{tagGuid}") {
        fun createRoute(tagGuid: String) = "tag_notes/$tagGuid"
        const val ARG_TAG_GUID = "tagGuid"
    }
}
