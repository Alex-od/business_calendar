package ua.danichapps.radiantdays.ai

interface AiApiRequestLogSink {
    fun save(log: String)
    fun get(): String?
    fun clear()
    fun hasLog(): Boolean
}
