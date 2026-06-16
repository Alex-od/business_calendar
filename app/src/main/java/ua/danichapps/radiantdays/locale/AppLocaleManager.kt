package ua.danichapps.radiantdays.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class AppLocaleManager {

    fun apply(tag: String?) {
        val locales = when (AppLanguages.resolveTag(tag)) {
            null -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(AppLanguages.resolveTag(tag))
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
