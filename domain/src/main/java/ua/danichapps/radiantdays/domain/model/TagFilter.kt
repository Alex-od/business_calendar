package ua.danichapps.radiantdays.domain.model

/** Returns true when [selectedGuids] is empty (no filter) or the event matches all selected tags (AND). */
fun CalendarEvent.matchesTagFilter(selectedGuids: Set<String>): Boolean {
    if (selectedGuids.isEmpty()) return true
    return selectedGuids.all { guid ->
        if (Tag.isUntaggedFilter(guid)) tagGuids.isEmpty() else guid in tagGuids
    }
}
