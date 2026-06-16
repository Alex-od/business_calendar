package ua.danichapps.radiantdays.locale

import ua.danichapps.radiantdays.R

object AppLanguages {
    val TAG_SYSTEM: String? = null
    const val TAG_RU = "ru"
    const val TAG_UK = "uk"
    const val TAG_EN = "en"

    val all: List<AppLanguageOption> = listOf(
        AppLanguageOption(TAG_SYSTEM, R.string.language_system),
        AppLanguageOption(TAG_RU, R.string.language_russian),
        AppLanguageOption(TAG_UK, R.string.language_ukrainian),
        AppLanguageOption(TAG_EN, R.string.language_english),
    )

    fun findByTag(tag: String?): AppLanguageOption? =
        all.find { it.tag == tag }

    fun resolveTag(raw: String?): String? = when (raw) {
        null, "", TAG_SYSTEM -> TAG_SYSTEM
        TAG_RU, TAG_UK, TAG_EN -> raw
        else -> TAG_SYSTEM
    }
}
