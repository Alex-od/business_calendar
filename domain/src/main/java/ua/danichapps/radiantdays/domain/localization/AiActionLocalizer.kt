package ua.danichapps.radiantdays.domain.localization

import ua.danichapps.radiantdays.domain.model.AiAction

interface AiActionLocalizer {
    fun localize(action: AiAction): AiAction
}

object PassthroughAiActionLocalizer : AiActionLocalizer {
    override fun localize(action: AiAction): AiAction = action
}
