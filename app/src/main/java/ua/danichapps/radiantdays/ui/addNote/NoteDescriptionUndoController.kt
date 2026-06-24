package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Bounded undo stack with typing-group debounce for the note description field. */
internal class NoteDescriptionUndoController(
    private val scope: CoroutineScope,
    private val undoLimit: Int = DEFAULT_UNDO_LIMIT,
    private val groupDelayMs: Long = DEFAULT_GROUP_DELAY_MS,
) {
    private val stack = ArrayDeque<String>()
    private var groupBase: String? = null
    private var groupJob: Job? = null
    private var isUndoing = false

    fun clear(onCanUndoChange: (Boolean) -> Unit) {
        groupJob?.cancel()
        groupJob = null
        groupBase = null
        stack.clear()
        onCanUndoChange(false)
    }

    fun onTypingChange(
        newValue: String,
        current: String,
        onCanUndoChange: (Boolean) -> Unit,
    ): String? {
        if (isUndoing) return newValue.takeIf { it != current }
        if (newValue == current) return null
        if (groupBase == null) {
            groupBase = current
            syncCanUndo(onCanUndoChange)
        }
        scheduleGroupCommit(onCanUndoChange)
        return newValue
    }

    fun onDiscreteChange(
        newValue: String,
        current: String,
        onCanUndoChange: (Boolean) -> Unit,
    ): String? {
        if (newValue == current) return null
        flushGroup(onCanUndoChange)
        pushSnapshot(current, onCanUndoChange)
        groupBase = null
        return newValue
    }

    fun undo(onCanUndoChange: (Boolean) -> Unit): String? {
        groupJob?.cancel()
        groupJob = null

        groupBase?.let { base ->
            groupBase = null
            syncCanUndo(onCanUndoChange)
            return applyUndo(base)
        }

        if (stack.isEmpty()) return null

        flushGroup(onCanUndoChange)
        val previous = stack.removeLast()
        syncCanUndo(onCanUndoChange)
        return applyUndo(previous)
    }

    private fun applyUndo(description: String): String {
        isUndoing = true
        return try {
            description
        } finally {
            isUndoing = false
        }
    }

    private fun scheduleGroupCommit(onCanUndoChange: (Boolean) -> Unit) {
        groupJob?.cancel()
        groupJob = scope.launch {
            delay(groupDelayMs)
            val base = groupBase ?: return@launch
            pushSnapshot(base, onCanUndoChange)
            groupBase = null
            syncCanUndo(onCanUndoChange)
            groupJob = null
        }
    }

    private fun pushSnapshot(text: String, onCanUndoChange: (Boolean) -> Unit) {
        if (stack.lastOrNull() == text) return
        if (stack.size >= undoLimit) {
            stack.removeFirst()
        }
        stack.addLast(text)
        syncCanUndo(onCanUndoChange)
    }

    private fun flushGroup(onCanUndoChange: (Boolean) -> Unit) {
        groupJob?.cancel()
        groupJob = null
        val base = groupBase
        if (base != null) {
            pushSnapshot(base, onCanUndoChange)
            groupBase = null
            syncCanUndo(onCanUndoChange)
        }
    }

    private fun syncCanUndo(onCanUndoChange: (Boolean) -> Unit) {
        onCanUndoChange(stack.isNotEmpty() || groupBase != null)
    }

    private companion object {
        const val DEFAULT_UNDO_LIMIT = 10
        const val DEFAULT_GROUP_DELAY_MS = 1_000L
    }
}
