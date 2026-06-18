package ua.danichapps.radiantdays.locale

import android.content.Context
import ua.danichapps.radiantdays.R

class AppStrings(context: Context) {

    private val appContext = context.applicationContext

    fun settingsModelChanged(): String = appContext.getString(R.string.settings_snackbar_model_changed)

    fun settingsEnterApiKey(): String = appContext.getString(R.string.settings_snackbar_enter_api_key)

    fun settingsKeySaved(): String = appContext.getString(R.string.settings_snackbar_key_saved)

    fun settingsKeyRemoved(): String = appContext.getString(R.string.settings_snackbar_key_removed)

    fun settingsApiCheckFailedTitle(): String =
        appContext.getString(R.string.settings_api_check_failed_title)

    fun settingsAiConnected(): String = appContext.getString(R.string.settings_ai_status_connected)

    fun aiStubResponse(preview: String): String =
        appContext.getString(R.string.ai_stub_response, preview)
}
