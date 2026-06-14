package ua.danichapps.radiantdays.domain.model

/** User-defined tag for grouping notes. A note may have zero or many tags. */
data class Tag(
    val guid: String,
    val name: String,
    val isPinned: Boolean = false,
    val color: EventColor = EventColor.DEFAULT,
) {
    val isUntaggedFilter: Boolean get() = isUntaggedFilter(guid)

    companion object {
        /** Virtual filter id for notes with no tags (not stored in DB). */
        const val UNTAGGED_GUID = "__untagged__"
        const val UNTAGGED_NAME = "Без тегов"

        fun untaggedFilter(): Tag = Tag(guid = UNTAGGED_GUID, name = UNTAGGED_NAME)

        fun isUntaggedFilter(guid: String): Boolean = guid == UNTAGGED_GUID
    }
}
