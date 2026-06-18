package ua.danichapps.radiantdays.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiApiKeySanitizerTest {

    @Test
    fun `removes carriage return and newline`() {
        val raw = "sk-test\r\n"

        val sanitized = AiApiKeySanitizer.sanitize(raw)

        assertEquals("sk-test", sanitized)
    }

    @Test
    fun `removes embedded control characters`() {
        val raw = "sk-\rproj-key"

        val sanitized = AiApiKeySanitizer.sanitize(raw)

        assertEquals("sk-proj-key", sanitized)
    }
}
