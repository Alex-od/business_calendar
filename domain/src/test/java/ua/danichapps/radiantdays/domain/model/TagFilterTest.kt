package ua.danichapps.radiantdays.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagFilterTest {

    private fun event(vararg tagGuids: String) = CalendarEvent(
        description = "body",
        startTimeMillis = 0L,
        endTimeMillis = 0L,
        tagGuids = tagGuids.toSet(),
    )

    @Test
    fun emptyFilter_matchesAll() {
        assertTrue(event("a").matchesTagFilter(emptySet()))
    }

    @Test
    fun singleTag_matchesWhenPresent() {
        assertTrue(event("a").matchesTagFilter(setOf("a")))
        assertFalse(event("a").matchesTagFilter(setOf("b")))
    }

    @Test
    fun andLogic_requiresAllTags() {
        assertTrue(event("a", "b").matchesTagFilter(setOf("a", "b")))
        assertFalse(event("a").matchesTagFilter(setOf("a", "b")))
        assertTrue(event("a", "b", "c").matchesTagFilter(setOf("a", "b")))
    }

    @Test
    fun untaggedFilter_matchesOnlyNotesWithoutTags() {
        assertTrue(event().matchesTagFilter(setOf(Tag.UNTAGGED_GUID)))
        assertFalse(event("a").matchesTagFilter(setOf(Tag.UNTAGGED_GUID)))
    }

    @Test
    fun untaggedPlusRealTag_neverMatches() {
        val filter = setOf(Tag.UNTAGGED_GUID, "a")
        assertFalse(event().matchesTagFilter(filter))
        assertFalse(event("a").matchesTagFilter(filter))
    }
}
