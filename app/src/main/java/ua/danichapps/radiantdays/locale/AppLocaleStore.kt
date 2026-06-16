package ua.danichapps.radiantdays.locale

import android.content.Context
import androidx.core.os.LocaleListCompat
import java.util.Locale

class AppLocaleStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    fun getTag(): String? = AppLanguages.resolveTag(prefs.getString(KEY_LANGUAGE_TAG, null))

    fun saveTag(tag: String?) {
        val resolved = AppLanguages.resolveTag(tag)
        prefs.edit().apply {
            if (resolved == null) {
                remove(KEY_LANGUAGE_TAG)
            } else {
                putString(KEY_LANGUAGE_TAG, resolved)
            }
        }.apply()
    }

    fun resolveLocale(): Locale {
        val tag = getTag() ?: return systemLocale()
        return Locale.forLanguageTag(tag)
    }

    fun resolveLocale(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) resolveLocale() else locales[0]
    }

    private fun systemLocale(): Locale {
        val locales = LocaleListCompat.getAdjustedDefault()
        return if (locales.isEmpty) Locale.getDefault() else locales[0] ?: Locale.getDefault()
    }

    private companion object {
        const val PREFS_NAME = "locale_prefs"
        const val KEY_LANGUAGE_TAG = "app_language_tag"
    }
}
