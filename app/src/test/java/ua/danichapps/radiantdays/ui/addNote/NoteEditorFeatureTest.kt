package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.danichapps.radiantdays.domain.model.Tag

class NoteEditorFeatureTest {

    @Test
    fun `discrete description change updates state and enables undo`() {
        var state = AddEditNoteUiState(description = "old")
        var autoSaveCalls = 0
        val feature = NoteEditorFeature(
            scope = TestScope(),
            readState = { state },
            updateState = { transform -> state = transform(state) },
            onScheduleAutoSave = { autoSaveCalls += 1 },
        )

        feature.onDescriptionChangeFromVoice("new")

        assertEquals("new", state.description)
        assertTrue(state.canUndoDescription)
        assertEquals(1, autoSaveCalls)
    }

    @Test
    fun `apply available tags keeps only valid selected guids`() {
        var state = AddEditNoteUiState(
            selectedTagGuids = setOf("work", "obsolete"),
        )
        val feature = NoteEditorFeature(
            scope = TestScope(),
            readState = { state },
            updateState = { transform -> state = transform(state) },
            onScheduleAutoSave = {},
        )
        val tags = listOf(
            Tag(guid = "work", name = "Work"),
            Tag(guid = "home", name = "Home"),
        )

        feature.applyAvailableTags(tags)

        assertEquals(tags, state.tags)
        assertEquals(setOf("work"), state.selectedTagGuids)
        assertFalse("obsolete" in state.selectedTagGuids)
    }
}
