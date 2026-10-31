package com.grk.mpa.app.core

import kotlin.random.Random

/** One generated puzzle shared by the Sudoku tests, so each test does not generate its own. */
object SudokuFixture {
    val generated: SudokuPuzzle by lazy { SudokuGenerator.generate(SudokuDifficulty.EASY, Random(7)) }

    fun game(difficulty: SudokuDifficulty = SudokuDifficulty.EASY): SudokuGame =
        SudokuOperations.newGame(difficulty, generated)

    /** The first cell that is empty in the puzzle. */
    val firstEmpty: Int get() = generated.puzzle.indexOfFirst { it == 0 }

    /** A game that is one correct number away from finished: the cell to fill is [lastCell]. */
    fun almostSolved(): Pair<SudokuGame, Int> {
        val lastCell = firstEmpty
        val values = generated.solution.toMutableList().also { it[lastCell] = 0 }
        return game().copy(values = values, selected = lastCell) to lastCell
    }
}
