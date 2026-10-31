package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.SudokuDifficulty
import com.grk.mpa.app.core.SudokuGame
import com.grk.mpa.app.core.SudokuGenerator
import com.grk.mpa.app.core.SudokuOperations
import com.grk.mpa.app.core.SudokuStatus
import com.grk.mpa.app.core.SudokuStep
import com.grk.mpa.app.core.SudokuStore
import com.grk.mpa.app.data.SudokuRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 * @param generationDispatcher where new puzzles are made, so the screen never freezes.
 * @param clock milliseconds from a clock that only moves forward.
 */
class SudokuViewModel(
    private val repository: SudokuRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val random: Random = Random.Default,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 }
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var difficulty by mutableStateOf(SudokuDifficulty.MEDIUM)
        private set

    /** The game on the board, or null while a new puzzle is being made. */
    var game by mutableStateOf<SudokuGame?>(null)
        private set
    var notesMode by mutableStateOf(false)
        private set
    var status by mutableStateOf<SudokuStatus>(SudokuStatus.Prompt)
        private set
    var bestMs by mutableStateOf<Map<SudokuDifficulty, Long>>(emptyMap())
        private set

    /** The time shown on screen. Call [refreshTime] about once a second. */
    var elapsedMs by mutableLongStateOf(0L)
        private set

    private var store = SudokuStore()
    private var isLoaded = false
    private var isActive = false
    private var segmentStart: Long? = null
    private var generationJob: Job? = null

    init {
        scope.launch {
            store = repository.load()
            bestMs = store.bestMs
            isLoaded = true
            startPuzzle(SudokuDifficulty.MEDIUM, forceNew = false)
        }
    }

    // ----- Starting puzzles -----

    fun onDifficultyChange(newDifficulty: SudokuDifficulty) {
        if (!isLoaded || newDifficulty == difficulty) return
        persist()
        startPuzzle(newDifficulty, forceNew = false)
    }

    fun newPuzzle() {
        if (!isLoaded) return
        startPuzzle(difficulty, forceNew = true)
    }

    private fun startPuzzle(newDifficulty: SudokuDifficulty, forceNew: Boolean) {
        generationJob?.cancel()
        difficulty = newDifficulty
        val saved = if (forceNew) null else store.games[newDifficulty]
        if (saved != null) {
            showNewGame(SudokuOperations.fromSave(saved))
            return
        }
        pauseClock()
        game = null
        generationJob = scope.launch {
            val generated = withContext(generationDispatcher) { SudokuGenerator.generate(newDifficulty, random) }
            showNewGame(SudokuOperations.newGame(newDifficulty, generated))
            persist()
        }
    }

    private fun showNewGame(newGame: SudokuGame) {
        segmentStart = null
        game = newGame
        notesMode = false
        status = SudokuStatus.Prompt
        resumeClock()
    }

    // ----- Moves -----

    fun select(index: Int) {
        game?.let { game = SudokuOperations.select(it, index) }
    }

    fun enter(number: Int) {
        game?.let { applyStep(SudokuOperations.enter(it, number, notesMode)) }
    }

    fun toggleNotes() {
        notesMode = !notesMode
    }

    fun undo() {
        game?.let { applyStep(SudokuOperations.undo(it)) }
    }

    fun check() {
        game?.let { applyStep(SudokuOperations.check(it)) }
    }

    fun hint() {
        game?.let { current -> SudokuOperations.hint(current)?.let { applyStep(it) } }
    }

    fun reset() {
        val current = game ?: return
        segmentStart = null
        applyStep(SudokuOperations.reset(current))
        resumeClock()
    }

    private fun applyStep(step: SudokuStep) {
        var updated = step.game
        var newStatus = step.status
        if (!updated.solved && SudokuOperations.isComplete(updated)) {
            val elapsed = updated.elapsedMs + runningSegment()
            segmentStart = null
            updated = updated.copy(solved = true, elapsedMs = elapsed)
            val best = bestMs[updated.difficulty]
            // Best times count only games played without hints.
            val newBest = updated.hints == 0 && (best == null || elapsed < best)
            if (newBest) bestMs = bestMs + (updated.difficulty to maxOf(elapsed, 1L))
            // A finished board must not come back the next time the app opens.
            store = store.copy(games = store.games - updated.difficulty, bestMs = bestMs)
            saveStore()
            newStatus = SudokuStatus.Completed(elapsed, updated.hints, newBest)
        }
        game = updated
        if (newStatus != null) status = newStatus
        refreshTime()
        if (!updated.solved) persist()
    }

    // ----- Clock: counts only while the screen is showing and the puzzle is unsolved -----

    fun onStart() {
        isActive = true
        resumeClock()
    }

    fun onStop() {
        isActive = false
        pauseClock()
        persist()
    }

    fun refreshTime() {
        elapsedMs = (game?.elapsedMs ?: 0L) + runningSegment()
    }

    private fun runningSegment(): Long = segmentStart?.let { clock() - it } ?: 0L

    private fun resumeClock() {
        val current = game
        if (isActive && current != null && !current.solved && segmentStart == null) segmentStart = clock()
        refreshTime()
    }

    private fun pauseClock() {
        val current = game
        val start = segmentStart
        if (current != null && start != null) game = current.copy(elapsedMs = current.elapsedMs + (clock() - start))
        segmentStart = null
        refreshTime()
    }

    // ----- Saving -----

    private fun persist() {
        val current = game ?: return
        if (current.solved) return
        val save = SudokuOperations.toSave(current, current.elapsedMs + runningSegment())
        store = store.copy(games = store.games + (current.difficulty to save), bestMs = bestMs)
        saveStore()
    }

    private fun saveStore() {
        val snapshot = store
        scope.launch { repository.save(snapshot) }
    }
}
