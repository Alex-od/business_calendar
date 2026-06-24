package ua.danichapps.radiantdays.data.local.seed

import ua.danichapps.radiantdays.data.local.entity.AiActionEntity
import ua.danichapps.radiantdays.domain.model.AiAction

/** Structural defaults for built-in AI actions. User-facing strings come from string resources. */
object BuiltinAiActions {
    private fun entry(guid: String, sortOrder: Int) = AiActionEntity(
        guid = guid,
        name = "",
        description = null,
        prompt = "",
        isVisible = true,
        sortOrder = sortOrder,
        isBuiltIn = true,
    )

    val all: List<AiActionEntity> = listOf(
        entry(AiAction.BUILTIN_IMPROVE_GUID, sortOrder = 0),
        entry(AiAction.BUILTIN_SHORTEN_GUID, sortOrder = 1),
        entry(AiAction.BUILTIN_CHECKLIST_GUID, sortOrder = 2),
    )
}
