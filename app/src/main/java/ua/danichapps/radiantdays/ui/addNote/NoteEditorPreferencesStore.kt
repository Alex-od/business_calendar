package ua.danichapps.radiantdays.ui.addNote

import android.content.Context

class NoteEditorPreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the format toolbar is shown in the note editor. */
    fun isFormatToolbarVisible(): Boolean =
        prefs.getBoolean(KEY_SHOW_FORMAT_TOOLBAR, DEFAULT_SHOW_FORMAT_TOOLBAR)

    /** Whether the inline AI chat panel is shown. */
    fun isAiChatVisible(): Boolean =
        prefs.getBoolean(KEY_SHOW_AI_CHAT, DEFAULT_SHOW_AI_CHAT)

    /** Persists format toolbar visibility. */
    fun setFormatToolbarVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FORMAT_TOOLBAR, visible).apply()
    }

    /** Persists AI chat panel visibility. */
    fun setAiChatVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_AI_CHAT, visible).apply()
    }

    private companion object {
        const val PREFS_NAME = "note_editor_prefs"
        const val KEY_SHOW_FORMAT_TOOLBAR = "show_format_toolbar"
        const val KEY_SHOW_AI_CHAT = "show_ai_chat"
        const val DEFAULT_SHOW_FORMAT_TOOLBAR = true
        const val DEFAULT_SHOW_AI_CHAT = true
    }
}
