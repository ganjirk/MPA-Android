package com.grk.mpa.app.core

import java.util.Locale

/** What the status line under the board says. The screen turns each case into text. */
sealed interface SudokuStatus {
    data object Prompt : SudokuStatus
    data object SelectEmpty : SudokuStatus
    data object Conflict : SudokuStatus
    data object Entered : SudokuStatus
    data object Cleared : SudokuStatus
    data object Undone : SudokuStatus
    data object Reset : SudokuStatus
    data object HintFilled : SudokuStatus
    data object AllCorrect : SudokuStatus
    data class Wrong(val count: Int) : SudokuStatus
    data class Completed(val elapsedMs: Long, val hints: Int, val newBest: Boolean) : SudokuStatus
}

/** What the board looked like before a move, so it can be undone. */
data class SudokuSnapshot(val values: List<Int>, val notes: List<Set<Int>>)

/** A game in progress. Cells are numbered 0 to 80, row by row. */
data class SudokuGame(
    val difficulty: SudokuDifficulty,
    val puzzle: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Set<Int>>,
    /** The selected cell, or -1 when none is. */
    val selected: Int,
    val hints: Int = 0,
    /** Playing time so far, not counting the part that is running right now. */
    val elapsedMs: Long = 0,
    val solved: Boolean = false,
    /** Cells marked wrong by the last Check. */
    val wrong: Set<Int> = emptySet(),
    val history: List<SudokuSnapshot> = emptyList()
) {
    val filledCount: Int get() = values.count { it != 0 }
    fun isGiven(index: Int): Boolean = puzzle[index] != 0
}

/** The new game and what to say about it. A null [status] leaves the status line as it was. */
data class SudokuStep(val game: SudokuGame, val status: SudokuStatus?)

object SudokuTime {
    /** mm:ss, as on the website. */
    fun format(ms: Long): String {
        val seconds = ms / 1000
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    }
}

/** Pure rules for the game, ported from the website's `sudoku.js`. */
object SudokuOperations {
    const val MAX_HISTORY = 100
    private val NO_NOTES: List<Set<Int>> = List(81) { emptySet() }

    fun fromSave(save: SudokuSave): SudokuGame = SudokuGame(
        difficulty = save.difficulty,
        puzzle = save.puzzle,
        solution = save.solution,
        values = save.values,
        notes = save.notes,
        selected = firstEditable(save.puzzle),
        hints = save.hints,
        elapsedMs = save.elapsedMs
    )

    /** What to keep when the app is closed. [elapsedMs] includes the part that is running right now. */
    fun toSave(game: SudokuGame, elapsedMs: Long): SudokuSave = SudokuSave(
        difficulty = game.difficulty,
        puzzle = game.puzzle,
        solution = game.solution,
        values = game.values,
        notes = game.notes,
        elapsedMs = elapsedMs,
        hints = game.hints
    )

    fun newGame(difficulty: SudokuDifficulty, generated: SudokuPuzzle): SudokuGame = SudokuGame(
        difficulty = difficulty,
        puzzle = generated.puzzle,
        solution = generated.solution,
        values = generated.puzzle,
        notes = NO_NOTES,
        selected = firstEditable(generated.puzzle)
    )

    private fun firstEditable(puzzle: List<Int>): Int = puzzle.indexOfFirst { it == 0 }

    /** True when [number] already appears in the same row, column or box as [index]. */
    fun conflict(values: List<Int>, index: Int, number: Int): Boolean {
        if (number == 0) return false
        val row = index / 9
        val col = index % 9
        for (k in 0 until 9) {
            if (row * 9 + k != index && values[row * 9 + k] == number) return true
            if (k * 9 + col != index && values[k * 9 + col] == number) return true
        }
        val boxRow = row / 3 * 3
        val boxCol = col / 3 * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                val j = r * 9 + c
                if (j != index && values[j] == number) return true
            }
        }
        return false
    }

    fun select(game: SudokuGame, index: Int): SudokuGame =
        if (index in 0..80) game.copy(selected = index) else game

    /**
     * Types [number] (0 erases) into the selected cell, or toggles it as a pencil note when
     * [notesMode] is on. Does nothing once the puzzle is solved.
     */
    fun enter(game: SudokuGame, number: Int, notesMode: Boolean): SudokuStep {
        if (game.solved) return SudokuStep(game, null)
        val cleared = game.copy(wrong = emptySet())
        val cell = game.selected
        if (cell < 0 || game.isGiven(cell)) return SudokuStep(cleared, SudokuStatus.SelectEmpty)

        val withHistory = cleared.copy(history = (cleared.history + snapshot(cleared)).takeLast(MAX_HISTORY))
        return if (notesMode && number != 0) {
            val current = withHistory.notes[cell]
            val updated = if (number in current) current - number else current + number
            SudokuStep(withHistory.copy(notes = withHistory.notes.replaced(cell, updated)), null)
        } else {
            writeValue(withHistory, cell, number)
        }
    }

    private fun writeValue(game: SudokuGame, cell: Int, number: Int): SudokuStep {
        val status = when {
            number == 0 -> SudokuStatus.Cleared
            conflict(game.values, cell, number) -> SudokuStatus.Conflict
            else -> SudokuStatus.Entered
        }
        val updated = game.copy(
            values = game.values.replaced(cell, number),
            notes = game.notes.replaced(cell, emptySet())
        )
        return SudokuStep(updated, status)
    }

    fun undo(game: SudokuGame): SudokuStep {
        if (game.solved) return SudokuStep(game, null)
        val last = game.history.lastOrNull() ?: return SudokuStep(game.copy(wrong = emptySet()), null)
        return SudokuStep(
            game.copy(
                values = last.values,
                notes = last.notes,
                history = game.history.dropLast(1),
                wrong = emptySet()
            ),
            SudokuStatus.Undone
        )
    }

    /** Starts the same puzzle again from the beginning. */
    fun reset(game: SudokuGame): SudokuStep = SudokuStep(
        game.copy(
            values = game.puzzle,
            notes = NO_NOTES,
            history = emptyList(),
            elapsedMs = 0,
            hints = 0,
            solved = false,
            wrong = emptySet()
        ),
        SudokuStatus.Reset
    )

    /**
     * Fills in one correct number. The selected cell is used when it can be edited and is not
     * already right; otherwise the first wrong or empty cell. Returns null when nothing is left to fill.
     * Unlike the website, a hint always writes the number, even while pencil notes are on.
     */
    fun hint(game: SudokuGame): SudokuStep? {
        if (game.solved) return null
        val selected = game.selected
        val selectedIsFixable =
            selected in 0..80 && !game.isGiven(selected) && game.values[selected] != game.solution[selected]
        val target: Int = if (selectedIsFixable) {
            selected
        } else {
            game.values.indices.firstOrNull { game.values[it] != game.solution[it] } ?: return null
        }

        val prepared = game.copy(
            selected = target,
            hints = game.hints + 1,
            wrong = emptySet(),
            history = (game.history + snapshot(game)).takeLast(MAX_HISTORY)
        )
        val step = writeValue(prepared, target, game.solution[target])
        return step.copy(status = SudokuStatus.HintFilled)
    }

    /** Marks every filled cell that differs from the solution. */
    fun check(game: SudokuGame): SudokuStep {
        if (game.solved) return SudokuStep(game, null)
        val wrong = game.values.indices.filter { game.values[it] != 0 && game.values[it] != game.solution[it] }.toSet()
        val status = if (wrong.isEmpty()) SudokuStatus.AllCorrect else SudokuStatus.Wrong(wrong.size)
        return SudokuStep(game.copy(wrong = wrong), status)
    }

    fun isComplete(game: SudokuGame): Boolean = game.values == game.solution

    private fun snapshot(game: SudokuGame) = SudokuSnapshot(game.values, game.notes)

    private fun <T> List<T>.replaced(index: Int, item: T): List<T> =
        toMutableList().also { it[index] = item }
}
