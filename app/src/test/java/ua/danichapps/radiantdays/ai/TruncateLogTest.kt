package ua.danichapps.radiantdays.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TruncateLogTest {

    @Test
    fun `returns original text when within limit`() {
        val text = "a".repeat(100)
        assertEquals(text, truncateLog(text, maxChars = 200))
    }

    @Test
    fun `truncates long text with suffix`() {
        val text = "x".repeat(300)
        val truncated = truncateLog(text, maxChars = 200)
        assertTrue(truncated.endsWith("... truncated"))
        assertEquals(200, truncated.length)
    }
}
