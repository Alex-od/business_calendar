package ua.danichapps.radiantdays.ai

object AiApiKeySanitizer {

    /** Removes whitespace and control chars often pasted into API keys. */
    fun sanitize(raw: String): String =
        raw.filter { character -> !character.isWhitespace() && !character.isISOControl() }
}
