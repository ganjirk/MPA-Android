package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {
    @Test
    fun fullGridIsAValidSolution() {
        repeat(5) { seed ->
            assertTrue(SudokuCodec.isValidSolution(SudokuGenerator.fullGrid(Random(seed)).toList()))
        }
    }

    @Test
    fun generatedPuzzlesHaveExactlyOneSolutionAndMatchTheirAnswer() {
        for (difficulty in SudokuDifficulty.entries) {
            repeat(3) { seed ->
                val result = SudokuGenerator.generate(difficulty, Random(seed))
                assertTrue(SudokuCodec.isValidSolution(result.solution))
                assertEquals(1, SudokuGenerator.countSolutions(result.puzzle.toIntArray(), 2))
                result.puzzle.forEachIndexed { i, v -> if (v != 0) assertEquals(result.solution[i], v) }
            }
        }
    }

    @Test
    fun cluesNeverGoBelowTheTargetAndSomeCellsAreRemoved() {
        for (difficulty in SudokuDifficulty.entries) {
            val clues = SudokuGenerator.generate(difficulty, Random(11)).puzzle.count { it != 0 }
            assertTrue("$difficulty has $clues clues", clues >= difficulty.clues)
            assertTrue("$difficulty has $clues clues", clues < 81)
        }
    }

    @Test
    fun theSameSeedGivesTheSamePuzzle() {
        val a = SudokuGenerator.generate(SudokuDifficulty.MEDIUM, Random(3))
        val b = SudokuGenerator.generate(SudokuDifficulty.MEDIUM, Random(3))
        assertEquals(a, b)
    }

    @Test
    fun countSolutionsHandlesEmptySolvedAndImpossibleGrids() {
        assertEquals(2, SudokuGenerator.countSolutions(IntArray(81), 2)) // stops at the limit
        val solved = SudokuGenerator.fullGrid(Random(1))
        assertEquals(1, SudokuGenerator.countSolutions(solved, 2))

        // Row 1 holds 1 to 8, and a 9 sits below its last cell, so that cell has no candidate.
        val impossible = IntArray(81)
        for (c in 0 until 8) impossible[c] = c + 1
        impossible[1 * 9 + 8] = 9
        assertEquals(0, SudokuGenerator.countSolutions(impossible, 2))
    }

    @Test
    fun countSolutionsDoesNotChangeItsInput() {
        val puzzle = SudokuFixture.generated.puzzle.toIntArray()
        val copy = puzzle.copyOf()
        SudokuGenerator.countSolutions(puzzle, 2)
        assertTrue(copy.contentEquals(puzzle))
    }

    @Test
    fun boxIndexMatchesTheBoard() {
        assertEquals(0, SudokuGenerator.boxOf(0))
        assertEquals(2, SudokuGenerator.boxOf(8))
        assertEquals(4, SudokuGenerator.boxOf(40))
        assertEquals(8, SudokuGenerator.boxOf(80))
        assertFalse((0 until 81).any { SudokuGenerator.boxOf(it) !in 0..8 })
    }
}
