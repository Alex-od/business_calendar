package ua.danichapps.radiantdays.ai

import android.content.Context
import ua.danichapps.radiantdays.BuildConfig

class AiApiRequestLogStore(context: Context) : AiApiRequestLogSink {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(log: String) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().putString(KEY_LAST_LOG, truncateLog(log)).apply()
    }

    override fun get(): String? = prefs.getString(KEY_LAST_LOG, null)?.takeIf { it.isNotEmpty() }

    override fun clear() {
        if (!BuildConfig.DEBUG) return
        prefs.edit().remove(KEY_LAST_LOG).apply()
    }

    override fun hasLog(): Boolean = !get().isNullOrBlank()

    private companion object {
        const val PREFS_NAME = "ai_request_log_prefs"
        const val KEY_LAST_LOG = "last_request_log"
    }
}

internal const val MAX_LOG_CHARS = 200_000
private const val TRUNCATED_SUFFIX = "\n... truncated"

internal fun truncateLog(text: String, maxChars: Int = MAX_LOG_CHARS): String {
    if (text.length <= maxChars) return text
    val keepLength = (maxChars - TRUNCATED_SUFFIX.length).coerceAtLeast(0)
    return text.take(keepLength) + TRUNCATED_SUFFIX
}
