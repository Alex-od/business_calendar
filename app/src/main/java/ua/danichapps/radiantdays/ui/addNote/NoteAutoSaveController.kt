package ua.danichapps.radiantdays.ui.addNote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Debounced auto-save for the add/edit note screen. */
internal class NoteAutoSaveController(
    private val scope: CoroutineScope,
    private val debounceMs: Long,
    private val readState: () -> AddEditNoteUiState,
    private val save: suspend (AddEditNoteUiState) -> Unit,
) {
    private var job: Job? = null

    fun schedule() {
        job?.cancel()
        job = scope.launch {
            delay(debounceMs)
            save(readState())
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    suspend fun flushAndSave() {
        job?.cancel()
        job = null
        save(readState())
    }
}
