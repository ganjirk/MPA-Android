package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuOperationsTest {
    private val ops = SudokuOperations
    private val solution = SudokuFixture.generated.solution

    private fun game() = SudokuFixture.game()

    /** An empty cell and a digit that is NOT the answer for it and does not conflict with anything. */
    private fun wrongButQuiet(g: SudokuGame): Pair<Int, Int> {
        for (cell in g.values.indices) {
            if (g.isGiven(cell)) continue
            for (n in 1..9) {
                if (n != solution[cell] && !ops.conflict(g.values, cell, n)) return cell to n
            }
        }
        error("no suitable cell")
    }

    @Test
    fun newGameStartsFromThePuzzleAndSelectsTheFirstEmptyCell() {
        val g = game()
        assertEquals(SudokuFixture.generated.puzzle, g.values)
        assertEquals(SudokuFixture.firstEmpty, g.selected)
        assertTrue(g.notes.all { it.isEmpty() })
        assertEquals(0, g.hints)
        assertFalse(g.solved)
    }

    @Test
    fun enteringANumberFillsTheCellAndClearsItsNotes() {
        val cell = SudokuFixture.firstEmpty
        var g = game()
        g = ops.enter(g, 4, notesMode = true).game
        assertEquals(setOf(4), g.notes[cell])
        val step = ops.enter(g, solution[cell], notesMode = false)
        assertEquals(solution[cell], step.game.values[cell])
        assertTrue(step.game.notes[cell].isEmpty())
        assertEquals(SudokuStatus.Entered, step.status)
    }

    @Test
    fun enteringANumberThatClashesIsAllowedButReported() {
        val g = game()
        val cell = g.selected
        val row = cell / 9
        val rowDigit = g.values.slice(row * 9 until row * 9 + 9).first { it != 0 }
        val step = ops.enter(g, rowDigit, notesMode = false)
        assertEquals(rowDigit, step.game.values[cell])
        assertEquals(SudokuStatus.Conflict, step.status)
    }

    @Test
    fun erasingClearsTheCell() {
        val cell = SudokuFixture.firstEmpty
        val filled = ops.enter(game(), solution[cell], false).game
        val step = ops.enter(filled, 0, false)
        assertEquals(0, step.game.values[cell])
        assertEquals(SudokuStatus.Cleared, step.status)
    }

    @Test
    fun givenCellsCannotBeChanged() {
        val g = game()
        val given = g.puzzle.indexOfFirst { it != 0 }
        val step = ops.enter(g.copy(selected = given), 5, false)
        assertEquals(g.values, step.game.values)
        assertEquals(SudokuStatus.SelectEmpty, step.status)
        assertTrue(step.game.history.isEmpty())
    }

    @Test
    fun noCellSelectedAsksForOne() {
        val step = ops.enter(game().copy(selected = -1), 5, false)
        assertEquals(SudokuStatus.SelectEmpty, step.status)
    }

    @Test
    fun pencilNotesToggleAndKeepTheStatusLine() {
        val cell = SudokuFixture.firstEmpty
        var step = ops.enter(game(), 3, notesMode = true)
        assertEquals(setOf(3), step.game.notes[cell])
        assertNull(step.status)
        step = ops.enter(step.game, 7, notesMode = true)
        assertEquals(setOf(3, 7), step.game.notes[cell])
        step = ops.enter(step.game, 3, notesMode = true)
        assertEquals(setOf(7), step.game.notes[cell])
        assertEquals(game().values, step.game.values) // notes never change the board
    }

    @Test
    fun eraseInNotesModeStillClearsTheCell() {
        val cell = SudokuFixture.firstEmpty
        val filled = ops.enter(game(), solution[cell], false).game
        val step = ops.enter(filled, 0, notesMode = true)
        assertEquals(0, step.game.values[cell])
    }

    @Test
    fun enteringClearsTheCheckMarks() {
        val g = game().copy(wrong = setOf(1, 2))
        assertTrue(ops.enter(g, 5, false).game.wrong.isEmpty())
    }

    @Test
    fun undoRestoresTheBoardAndNotesStepByStep() {
        val cell = SudokuFixture.firstEmpty
        val a = ops.enter(game(), 2, notesMode = true).game
        val b = ops.enter(a, solution[cell], notesMode = false).game
        val undone = ops.undo(b)
        assertEquals(SudokuStatus.Undone, undone.status)
        assertEquals(a.values, undone.game.values)
        assertEquals(a.notes, undone.game.notes)
        val undoneAgain = ops.undo(undone.game)
        assertEquals(game().notes, undoneAgain.game.notes)
    }

    @Test
    fun undoWithNothingToUndoChangesNothing() {
        val step = ops.undo(game())
        assertNull(step.status)
        assertEquals(game(), step.game)
    }

    @Test
    fun historyKeepsOnlyTheLastHundredMoves() {
        var g = game()
        repeat(130) { i -> g = ops.enter(g, i % 9 + 1, notesMode = true).game }
        assertEquals(SudokuOperations.MAX_HISTORY, g.history.size)
    }

    @Test
    fun resetGoesBackToThePuzzleAndForgetsTimeAndHints() {
        val cell = SudokuFixture.firstEmpty
        var g = ops.enter(game(), solution[cell], false).game.copy(hints = 3, elapsedMs = 90_000)
        val step = ops.reset(g)
        assertEquals(SudokuStatus.Reset, step.status)
        assertEquals(g.puzzle, step.game.values)
        assertEquals(0, step.game.hints)
        assertEquals(0L, step.game.elapsedMs)
        assertTrue(step.game.history.isEmpty())
        assertTrue(step.game.notes.all { it.isEmpty() })
    }

    @Test
    fun hintFillsTheSelectedCellWithTheAnswer() {
        val g = game()
        val step = ops.hint(g)!!
        assertEquals(solution[g.selected], step.game.values[g.selected])
        assertEquals(1, step.game.hints)
        assertEquals(SudokuStatus.HintFilled, step.status)
    }

    @Test
    fun hintWritesTheNumberEvenWhenTheCellHadNotes() {
        val g = ops.enter(game(), 9, notesMode = true).game
        val step = ops.hint(g)!!
        assertEquals(solution[g.selected], step.game.values[g.selected])
        assertTrue(step.game.notes[g.selected].isEmpty())
    }

    @Test
    fun hintCanBeUndoneButTheHintStaysCounted() {
        val step = ops.hint(game())!!
        val undone = ops.undo(step.game).game
        assertEquals(game().values, undone.values)
        assertEquals(1, undone.hints)
    }

    @Test
    fun hintSkipsASelectedCellThatIsAlreadyRight() {
        val g = game()
        val first = g.selected
        val done = ops.enter(g, solution[first], false).game // selected cell is now correct
        val step = ops.hint(done)!!
        val filledCell = step.game.selected
        assertNotEquals(first, filledCell)
        assertEquals(solution[filledCell], step.game.values[filledCell])
    }

    @Test
    fun hintReplacesAWrongNumberInTheSelectedCell() {
        val g = game()
        val (cell, wrong) = wrongButQuiet(g)
        val withWrong = ops.enter(g.copy(selected = cell), wrong, false).game
        val step = ops.hint(withWrong)!!
        assertEquals(cell, step.game.selected)
        assertEquals(solution[cell], step.game.values[cell])
    }

    @Test
    fun hintDoesNothingWhenThePuzzleIsComplete() {
        val complete = game().copy(values = solution)
        assertNull(ops.hint(complete))
    }

    @Test
    fun checkMarksOnlyFilledCellsThatAreWrong() {
        val g = game()
        val (cell, wrong) = wrongButQuiet(g)
        val withWrong = ops.enter(g.copy(selected = cell), wrong, false).game
        val step = ops.check(withWrong)
        assertEquals(setOf(cell), step.game.wrong)
        assertEquals(SudokuStatus.Wrong(1), step.status)
    }

    @Test
    fun checkReportsWhenEverythingSoFarIsRight() {
        val g = game()
        val ok = ops.enter(g, solution[g.selected], false).game
        val step = ops.check(ok)
        assertTrue(step.game.wrong.isEmpty())
        assertEquals(SudokuStatus.AllCorrect, step.status)
    }

    @Test
    fun movesAreIgnoredOnceSolved() {
        val solved = game().copy(values = solution, solved = true)
        assertEquals(solved, ops.enter(solved, 5, false).game)
        assertEquals(solved, ops.undo(solved).game)
        assertEquals(solved, ops.check(solved).game)
        assertNull(ops.hint(solved))
    }

    @Test
    fun completionIsDetectedWhenAllCellsMatchTheSolution() {
        val (almost, cell) = SudokuFixture.almostSolved()
        assertFalse(ops.isComplete(almost))
        assertTrue(ops.isComplete(ops.enter(almost, solution[cell], false).game))
    }

    @Test
    fun conflictFindsClashesInRowColumnAndBoxButNotTheCellItself() {
        val values = MutableList(81) { 0 }
        values[0] = 5
        assertFalse(ops.conflict(values, 0, 5)) // itself
        assertTrue(ops.conflict(values, 8, 5))  // same row
        assertTrue(ops.conflict(values, 72, 5)) // same column
        assertTrue(ops.conflict(values, 20, 5)) // same box
        assertFalse(ops.conflict(values, 40, 5))
        assertFalse(ops.conflict(values, 8, 0)) // erasing never clashes
    }

    @Test
    fun selectIgnoresCellsOffTheBoard() {
        val g = game()
        assertEquals(10, ops.select(g, 10).selected)
        assertEquals(g.selected, ops.select(g, 81).selected)
        assertEquals(g.selected, ops.select(g, -1).selected)
    }

    @Test
    fun saveAndRestoreKeepTheGame() {
        val cell = SudokuFixture.firstEmpty
        val played = ops.enter(ops.enter(game(), 2, true).game, solution[cell], false).game.copy(hints = 2)
        val restored = ops.fromSave(ops.toSave(played, elapsedMs = 12_345))
        assertEquals(played.values, restored.values)
        assertEquals(played.notes, restored.notes)
        assertEquals(2, restored.hints)
        assertEquals(12_345L, restored.elapsedMs)
        assertTrue(restored.history.isEmpty())
        assertEquals(SudokuFixture.firstEmpty, restored.selected)
    }

    @Test
    fun timeIsShownAsMinutesAndSeconds() {
        assertEquals("00:00", SudokuTime.format(0))
        assertEquals("00:59", SudokuTime.format(59_999))
        assertEquals("01:05", SudokuTime.format(65_000))
        assertEquals("75:00", SudokuTime.format(75 * 60_000L))
    }
}
