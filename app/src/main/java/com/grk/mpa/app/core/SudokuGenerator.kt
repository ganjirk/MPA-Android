package com.grk.mpa.app.core

import kotlin.random.Random

enum class SudokuDifficulty(val clues: Int) {
    EASY(40),
    MEDIUM(32),
    HARD(26)
}

/** A puzzle and its solution. Both are 81 numbers, row by row; 0 means an empty cell. */
data class SudokuPuzzle(val puzzle: List<Int>, val solution: List<Int>)

/**
 * Ported from the website's `sudoku-generator.js`: build a random solved grid, then remove cells
 * one at a time, keeping a removal only while the puzzle still has exactly one solution.
 */
object SudokuGenerator {
    private const val ALL_DIGITS = 0x1FF // bits 0 to 8 stand for the digits 1 to 9

    fun boxOf(index: Int): Int = (index / 27) * 3 + (index % 9) / 3

    fun generate(difficulty: SudokuDifficulty, random: Random = Random.Default): SudokuPuzzle {
        val solution = fullGrid(random)
        val puzzle = solution.copyOf()
        var clues = 81
        for (index in (0 until 81).shuffled(random)) {
            if (clues <= difficulty.clues) break
            val kept = puzzle[index]
            puzzle[index] = 0
            if (countSolutions(puzzle, 2) == 1) clues-- else puzzle[index] = kept
        }
        return SudokuPuzzle(puzzle.toList(), solution.toList())
    }

    /** Counts the solutions of [puzzle], stopping at [limit]. The array is not changed. */
    fun countSolutions(puzzle: IntArray, limit: Int = 2): Int = Solver(puzzle.copyOf(), limit, null).run()

    fun fullGrid(random: Random = Random.Default): IntArray {
        val grid = IntArray(81)
        Solver(grid, 1, random).run()
        return grid
    }

    /**
     * Backtracking solver that always fills the empty cell with the fewest candidates.
     * With a [random] it stops at the first solution and leaves it in [grid].
     */
    private class Solver(val grid: IntArray, val limit: Int, val random: Random?) {
        private val rows = IntArray(9)
        private val cols = IntArray(9)
        private val boxes = IntArray(9)
        private var count = 0

        init {
            for (i in 0 until 81) {
                if (grid[i] != 0) {
                    val bit = 1 shl (grid[i] - 1)
                    rows[i / 9] = rows[i / 9] or bit
                    cols[i % 9] = cols[i % 9] or bit
                    boxes[boxOf(i)] = boxes[boxOf(i)] or bit
                }
            }
        }

        fun run(): Int {
            step()
            return count
        }

        /** Returns true when the search should stop. */
        private fun step(): Boolean {
            var best = -1
            var bestMask = 0
            var bestCount = 10
            for (i in 0 until 81) {
                if (grid[i] != 0) continue
                val mask = ALL_DIGITS and (rows[i / 9] or cols[i % 9] or boxes[boxOf(i)]).inv()
                val n = Integer.bitCount(mask)
                if (n < bestCount) {
                    best = i
                    bestMask = mask
                    bestCount = n
                    if (n <= 1) break
                }
            }
            if (best == -1) {
                count++
                return random != null || count >= limit
            }
            if (bestCount == 0) return false

            var digits = (1..9).filter { bestMask and (1 shl (it - 1)) != 0 }
            if (random != null) digits = digits.shuffled(random)
            val r = best / 9
            val c = best % 9
            val b = boxOf(best)
            for (d in digits) {
                val bit = 1 shl (d - 1)
                grid[best] = d
                rows[r] = rows[r] or bit
                cols[c] = cols[c] or bit
                boxes[b] = boxes[b] or bit
                if (step()) return true
                grid[best] = 0
                rows[r] = rows[r] and bit.inv()
                cols[c] = cols[c] and bit.inv()
                boxes[b] = boxes[b] and bit.inv()
            }
            grid[best] = 0
            return false
        }
    }
}
