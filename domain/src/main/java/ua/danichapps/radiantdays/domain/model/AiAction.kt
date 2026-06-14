package ua.danichapps.radiantdays.domain.model

/** User-defined AI action shown in the note editor bottom sheet. */
data class AiAction(
    val guid: String,
    val name: String,
    val description: String? = null,
    val prompt: String,
    val isVisible: Boolean = true,
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
) {
    companion object {
        const val BUILTIN_IMPROVE_GUID = "a1000000-0000-4000-8000-000000000001"
        const val BUILTIN_SHORTEN_GUID = "a1000000-0000-4000-8000-000000000002"
        const val BUILTIN_CHECKLIST_GUID = "a1000000-0000-4000-8000-000000000003"
    }
}
