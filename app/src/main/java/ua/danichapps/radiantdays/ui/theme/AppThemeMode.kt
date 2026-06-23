package ua.danichapps.radiantdays.ui.theme

import androidx.annotation.StringRes
import ua.danichapps.radiantdays.R

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStored(value: String?): AppThemeMode = when (value) {
            LIGHT.name -> LIGHT
            DARK.name -> DARK
            else -> SYSTEM
        }
    }
}

data class AppThemeOption(
    val mode: AppThemeMode,
    @param:StringRes val labelRes: Int,
)

object AppThemeModes {
    val all: List<AppThemeOption> = listOf(
        AppThemeOption(AppThemeMode.SYSTEM, R.string.theme_system),
        AppThemeOption(AppThemeMode.LIGHT, R.string.theme_light),
        AppThemeOption(AppThemeMode.DARK, R.string.theme_dark),
    )

    fun findByMode(mode: AppThemeMode): AppThemeOption =
        all.first { it.mode == mode }
}
