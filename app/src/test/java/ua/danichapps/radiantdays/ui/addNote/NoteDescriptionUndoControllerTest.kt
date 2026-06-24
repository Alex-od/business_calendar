package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDescriptionUndoControllerTest {

    @Test
    fun `clear resets undo availability`() {
        val controller = NoteDescriptionUndoController(TestScope(), groupDelayMs = 1_000)
        var canUndo = false

        controller.onDiscreteChange("b", "a") { canUndo = it }
        assertTrue(canUndo)

        controller.clear { canUndo = it }
        assertFalse(canUndo)
        assertNull(controller.undo { })
    }

    @Test
    fun `discrete change pushes snapshot and undo restores it`() {
        val controller = NoteDescriptionUndoController(TestScope(), groupDelayMs = 1_000)
        var canUndo = false
        val onCanUndo = { value: Boolean -> canUndo = value }

        val next = controller.onDiscreteChange("second", "first", onCanUndo)
        assertEquals("second", next)
        assertTrue(canUndo)

        val previous = controller.undo(onCanUndo)
        assertEquals("first", previous)
        assertFalse(canUndo)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `typing change commits group after debounce`() = runTest {
        val controller = NoteDescriptionUndoController(this, groupDelayMs = 500)
        var canUndo = false
        val onCanUndo = { value: Boolean -> canUndo = value }

        val next = controller.onTypingChange("ab", "a", onCanUndo)
        assertEquals("ab", next)
        assertTrue(canUndo)

        advanceTimeBy(500)

        val restored = controller.undo(onCanUndo)
        assertEquals("a", restored)
    }

    @Test
    fun `undo evicts oldest snapshot when limit exceeded`() {
        val controller = NoteDescriptionUndoController(
            scope = TestScope(),
            undoLimit = 2,
            groupDelayMs = 1_000,
        )
        var canUndo = false
        val onCanUndo = { value: Boolean -> canUndo = value }

        controller.onDiscreteChange("b", "a", onCanUndo)
        controller.onDiscreteChange("c", "b", onCanUndo)
        controller.onDiscreteChange("d", "c", onCanUndo)

        assertEquals("c", controller.undo(onCanUndo))
        assertEquals("b", controller.undo(onCanUndo))
        assertNull(controller.undo(onCanUndo))
        assertFalse(canUndo)
    }

    @Test
    fun `typing change with same value is ignored`() {
        val controller = NoteDescriptionUndoController(TestScope(), groupDelayMs = 1_000)
        var canUndo = false

        val next = controller.onTypingChange("same", "same") { canUndo = it }

        assertNull(next)
        assertFalse(canUndo)
    }
}
