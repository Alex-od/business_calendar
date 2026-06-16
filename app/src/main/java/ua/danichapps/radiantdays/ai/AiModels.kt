package ua.danichapps.radiantdays.ai

import ua.danichapps.radiantdays.R

data class AiModelOption(
    val id: String,
    val displayName: String,
    val descriptionRes: Int,
)

object AiModels {
    const val DEFAULT_ID = "gpt-5-mini"

    val options: List<AiModelOption> = listOf(
        AiModelOption(
            id = "gpt-5-nano",
            displayName = "GPT-5 Nano",
            descriptionRes = R.string.ai_model_nano_desc,
        ),
        AiModelOption(
            id = "gpt-5-mini",
            displayName = "GPT-5 Mini",
            descriptionRes = R.string.ai_model_mini_desc,
        ),
        AiModelOption(
            id = "gpt-5",
            displayName = "GPT-5",
            descriptionRes = R.string.ai_model_full_desc,
        ),
    )

    fun findById(id: String): AiModelOption? = options.find { it.id == id }

    fun resolveId(id: String?): String = findById(id.orEmpty())?.id ?: DEFAULT_ID
}
