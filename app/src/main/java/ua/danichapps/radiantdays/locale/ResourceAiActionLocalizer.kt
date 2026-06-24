package ua.danichapps.radiantdays.locale

import ua.danichapps.radiantdays.domain.localization.AiActionLocalizer
import ua.danichapps.radiantdays.domain.model.AiAction

class ResourceAiActionLocalizer(
    private val builtinStrings: BuiltinAiActionStrings,
) : AiActionLocalizer {

    override fun localize(action: AiAction): AiAction {
        if (!action.isBuiltIn) return action
        val strings = builtinStrings.resolve(action.guid) ?: return action
        return action.copy(
            name = strings.name,
            description = strings.description,
            prompt = strings.prompt,
        )
    }
}
