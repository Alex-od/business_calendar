package ua.danichapps.radiantdays.locale

import android.content.Context
import ua.danichapps.radiantdays.R
import ua.danichapps.radiantdays.domain.model.AiAction

class BuiltinAiActionStrings(context: Context) {

    private val appContext = context.applicationContext

    data class Strings(
        val name: String,
        val description: String?,
        val prompt: String,
    )

    fun resolve(guid: String): Strings? = when (guid) {
        AiAction.BUILTIN_IMPROVE_GUID -> Strings(
            name = appContext.getString(R.string.ai_action_builtin_improve_name),
            description = appContext.getString(R.string.ai_action_builtin_improve_description),
            prompt = appContext.getString(R.string.ai_action_builtin_improve_prompt),
        )
        AiAction.BUILTIN_SHORTEN_GUID -> Strings(
            name = appContext.getString(R.string.ai_action_builtin_shorten_name),
            description = appContext.getString(R.string.ai_action_builtin_shorten_description),
            prompt = appContext.getString(R.string.ai_action_builtin_shorten_prompt),
        )
        AiAction.BUILTIN_CHECKLIST_GUID -> Strings(
            name = appContext.getString(R.string.ai_action_builtin_checklist_name),
            description = appContext.getString(R.string.ai_action_builtin_checklist_description),
            prompt = appContext.getString(R.string.ai_action_builtin_checklist_prompt),
        )
        else -> null
    }
}
