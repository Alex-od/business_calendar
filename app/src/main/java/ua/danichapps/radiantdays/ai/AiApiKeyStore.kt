package ua.danichapps.radiantdays.ai

import android.content.Context

class AiApiKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getKey(): String? = prefs.getString(KEY_OPENAI_API, null)

    fun saveKey(key: String) {
        prefs.edit().putString(KEY_OPENAI_API, key.trim()).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_OPENAI_API).apply()
    }

    fun hasKey(): Boolean = !getKey().isNullOrBlank()

    private companion object {
        const val PREFS_NAME = "ai_prefs"
        const val KEY_OPENAI_API = "openai_api_key"
    }
}
