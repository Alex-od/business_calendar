package ua.danichapps.radiantdays.ai

class RecordingLogSink : AiApiRequestLogSink {
    var savedLog: String? = null
        private set

    override fun save(log: String) {
        savedLog = log
    }

    override fun get(): String? = savedLog

    override fun clear() {
        savedLog = null
    }

    override fun hasLog(): Boolean = !savedLog.isNullOrBlank()
}
