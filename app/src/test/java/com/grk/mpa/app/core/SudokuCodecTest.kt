package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuCodecTest {
    private val puzzle = SudokuFixture.generated.puzzle
    private val solution = SudokuFixture.generated.solution

    private fun save(
        difficulty: SudokuDifficulty = SudokuDifficulty.EASY,
        values: List<Int> = puzzle,
        notes: List<Set<Int>> = List(81) { emptySet() },
        elapsed: Long = 1000,
        hints: Int = 0
    ) = SudokuSave(difficulty, puzzle, solution, values, notes, elapsed, hints)

    @Test
    fun emptyStoreRoundTrips() {
        assertEquals(SudokuStore(), SudokuCodec.decode(SudokuCodec.encode(SudokuStore())))
        assertEquals(SudokuStore(), SudokuCodec.decode(""))
    }

    @Test
    fun gamesNotesAndBestTimesRoundTrip() {
        val cell = SudokuFixture.firstEmpty
        val values = puzzle.toMutableList().also { it[cell] = solution[cell] }
        val notes = List(81) { i -> if (i == cell + 1 && puzzle[i] == 0) setOf(1, 5, 9) else emptySet() }
        val store = SudokuStore(
            games = mapOf(
                SudokuDifficulty.EASY to save(values = values, notes = notes, elapsed = 83_000, hints = 2),
                SudokuDifficulty.HARD to save(SudokuDifficulty.HARD)
            ),
            bestMs = mapOf(SudokuDifficulty.EASY to 61_000L, SudokuDifficulty.MEDIUM to 120_500L)
        )
        assertEquals(store, SudokuCodec.decode(SudokuCodec.encode(store)))
    }

    @Test
    fun aFinishedGameIsNotRestored() {
        val finished = SudokuStore(games = mapOf(SudokuDifficulty.EASY to save(values = solution)))
        assertTrue(SudokuCodec.decode(SudokuCodec.encode(finished)).games.isEmpty())
    }

    private fun gameLine(
        puzzleText: String = puzzle.joinToString(""),
        solutionText: String = solution.joinToString(""),
        valuesText: String = puzzle.joinToString(""),
        notesText: String = List(81) { "" }.joinToString(","),
        elapsed: String = "1000",
        hints: String = "0",
        difficulty: String = "EASY"
    ) = listOf("G", difficulty, puzzleText, solutionText, valuesText, notesText, elapsed, hints).joinToString("\t")

    @Test
    fun theTestLineBuilderProducesAReadableRecord() {
        assertEquals(1, SudokuCodec.decode(gameLine()).games.size)
    }

    @Test
    fun brokenGameRecordsAreSkipped() {
        val broken = listOf(
            gameLine(difficulty = "IMPOSSIBLE"),
            gameLine(puzzleText = puzzle.joinToString("").drop(1)),
            gameLine(valuesText = "x".repeat(81)),
            gameLine(solutionText = "1".repeat(81)),                       // not a valid solution
            gameLine(puzzleText = solution.joinToString("").replaceRange(0, 1, if (solution[0] == 9) "1" else "9")), // given contradicts answer
            gameLine(valuesText = puzzle.joinToString("").let { text ->    // a given was overwritten
                val g = puzzle.indexOfFirst { it != 0 }
                text.replaceRange(g, g + 1, if (puzzle[g] == 9) "1" else "9")
            }),
            gameLine(notesText = List(80) { "" }.joinToString(",")),      // too few note groups
            gameLine(notesText = (listOf("0") + List(80) { "" }).joinToString(",")), // note digit 0
            gameLine(elapsed = "-5"),
            gameLine(elapsed = "soon"),
            gameLine(hints = "-1"),
            "G\tEASY\ttoo\tfew\tfields"
        ).joinToString("\n")
        assertTrue(SudokuCodec.decode(broken).games.isEmpty())
    }

    @Test
    fun oneBadRecordDoesNotHideTheGoodOnes() {
        val text = listOf(gameLine(elapsed = "oops"), gameLine(difficulty = "HARD"), "B\tEASY\t5000").joinToString("\n")
        val store = SudokuCodec.decode(text)
        assertEquals(setOf(SudokuDifficulty.HARD), store.games.keys)
        assertEquals(mapOf(SudokuDifficulty.EASY to 5000L), store.bestMs)
    }

    @Test
    fun badBestTimesAreSkipped() {
        val text = listOf("B\tEASY\tabc", "B\tEASY\t0", "B\tEASY\t-4", "B\tNOPE\t5", "B\tEASY", "B\tHARD\t9000").joinToString("\n")
        assertEquals(mapOf(SudokuDifficulty.HARD to 9000L), SudokuCodec.decode(text).bestMs)
    }

    @Test
    fun solutionValidationChecksRowsColumnsAndBoxes() {
        assertTrue(SudokuCodec.isValidSolution(solution))
        assertFalse(SudokuCodec.isValidSolution(solution.take(80)))
        assertFalse(SudokuCodec.isValidSolution(List(81) { 1 }))
        // Swapping two cells in one row keeps the row valid but breaks columns or boxes.
        val swapped = solution.toMutableList().also { val t = it[0]; it[0] = it[1]; it[1] = t }
        assertFalse(SudokuCodec.isValidSolution(swapped))
    }
}
