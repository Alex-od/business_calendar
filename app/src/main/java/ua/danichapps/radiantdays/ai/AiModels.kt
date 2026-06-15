package ua.danichapps.radiantdays.ai

data class AiModelOption(
    val id: String,
    val displayName: String,
    val description: String,
)

object AiModels {
    const val DEFAULT_ID = "gpt-5-mini"

    val options: List<AiModelOption> = listOf(
        AiModelOption(
            id = "gpt-5-nano",
            displayName = "GPT-5 Nano",
            description = "Быстрая и экономичная",
        ),
        AiModelOption(
            id = "gpt-5-mini",
            displayName = "GPT-5 Mini",
            description = "Баланс скорости и качества",
        ),
        AiModelOption(
            id = "gpt-5",
            displayName = "GPT-5",
            description = "Максимальное качество",
        ),
    )

    fun findById(id: String): AiModelOption? = options.find { it.id == id }

    fun resolveId(id: String?): String = findById(id.orEmpty())?.id ?: DEFAULT_ID
}
