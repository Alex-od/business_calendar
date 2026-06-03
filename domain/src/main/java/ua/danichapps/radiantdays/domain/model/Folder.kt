package ua.danichapps.radiantdays.domain.model

/**
 * User-defined folder used to group notes/events.
 */
data class Folder(
    val guid: String,
    val name: String,
    val isPinned: Boolean = false,
) {
    val isGeneral: Boolean get() = Companion.isGeneral(guid)

    companion object {
        const val GENERAL_GUID = "__general__"
        const val GENERAL_NAME = "Общее"

        fun general(): Folder = Folder(guid = GENERAL_GUID, name = GENERAL_NAME)

        fun isGeneral(guid: String): Boolean = guid == GENERAL_GUID
    }
}
