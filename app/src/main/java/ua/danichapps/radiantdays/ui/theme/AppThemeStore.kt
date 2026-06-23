package ua.danichapps.radiantdays.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppThemeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<AppThemeMode> = _mode.asStateFlow()

    fun getMode(): AppThemeMode = _mode.value

    fun saveMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _mode.value = mode
    }

    fun resolveDarkTheme(systemInDarkTheme: Boolean): Boolean = when (getMode()) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    private fun loadMode(): AppThemeMode =
        AppThemeMode.fromStored(prefs.getString(KEY_THEME_MODE, null))

    private companion object {
        const val PREFS_NAME = "theme_prefs"
        const val KEY_THEME_MODE = "app_theme_mode"
    }
}
