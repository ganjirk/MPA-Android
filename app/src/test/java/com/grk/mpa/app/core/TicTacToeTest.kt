package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TicTacToeTest {
    private fun stateOf(vararg cells: Int): TicTacToeState =
        cells.fold(TicTacToeState()) { s, cell -> TicTacToe.play(s, cell) }

    /** A Random that always picks the first choice, so tests can predict the computer. */
    private val firstChoice = object : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    @Test
    fun everyWinningLineIsDetectedForBothMarks() {
        for (line in TicTacToe.WIN_LINES) {
            for (mark in Mark.entries) {
                val outcome = TicTacToe.outcome(line.associateWith { mark })
                assertEquals(mark, outcome.winner)
                assertEquals(line, outcome.line)
                assertFalse(outcome.isDraw)
            }
        }
    }

    @Test
    fun fullBoardWithoutALineIsADraw() {
        // X O X
        // X O O
        // O X X
        val board = mapOf(
            1 to Mark.X, 2 to Mark.O, 3 to Mark.X,
            4 to Mark.X, 5 to Mark.O, 6 to Mark.O,
            7 to Mark.O, 8 to Mark.X, 9 to Mark.X
        )
        val outcome = TicTacToe.outcome(board)
        assertTrue(outcome.isDraw)
        assertNull(outcome.winner)
        assertTrue(outcome.isOver)
    }

    @Test
    fun unfinishedBoardHasNoOutcome() {
        val outcome = TicTacToe.outcome(mapOf(1 to Mark.X, 5 to Mark.O))
        assertFalse(outcome.isOver)
        assertTrue(outcome.line.isEmpty())
    }

    @Test
    fun marksAlternateStartingWithX() {
        val state = stateOf(1, 5, 9)
        assertEquals(listOf(Mark.X, Mark.O, Mark.X), state.moves.map { it.mark })
        assertEquals(Mark.O, state.nextMark)
    }

    @Test
    fun invalidAndRepeatedMovesAreIgnored() {
        val state = stateOf(1)
        assertSame(state, TicTacToe.play(state, 1))
        assertSame(state, TicTacToe.play(state, 0))
        assertSame(state, TicTacToe.play(state, 10))
    }

    @Test
    fun noMovesAfterTheGameIsOver() {
        val won = stateOf(1, 4, 2, 5, 3)
        assertEquals(Mark.X, won.outcome.winner)
        assertEquals(listOf(1, 2, 3), won.outcome.line)
        assertSame(won, TicTacToe.play(won, 9))
    }

    @Test
    fun computerWinsWhenItCan() {
        // O has 1 and 2; X has 4 and 5 (and could win at 6). The computer takes its own win at 3.
        val board = mapOf(1 to Mark.O, 2 to Mark.O, 4 to Mark.X, 5 to Mark.X)
        assertEquals(3, TicTacToe.computerCell(board, firstChoice))
    }

    @Test
    fun computerBlocksTheHuman() {
        val board = mapOf(1 to Mark.X, 2 to Mark.X, 5 to Mark.O)
        assertEquals(3, TicTacToe.computerCell(board, firstChoice))
    }

    @Test
    fun computerTakesTheCentreWhenThereIsNoThreat() {
        assertEquals(5, TicTacToe.computerCell(mapOf(1 to Mark.X), firstChoice))
    }

    @Test
    fun computerTakesACornerWhenTheCentreIsTaken() {
        val board = mapOf(5 to Mark.X)
        repeat(50) { seed ->
            assertTrue(TicTacToe.computerCell(board, Random(seed)) in listOf(1, 3, 7, 9))
        }
        assertEquals(1, TicTacToe.computerCell(board, firstChoice))
    }

    @Test
    fun computerChecksLinesInOrderAndPrefersWinningToBlocking() {
        // O can win at 6 (3-6-9) or at 8 (7-8-9). Rows are checked before columns, so it plays 8,
        // exactly as the website does.
        val twoWins = mapOf(1 to Mark.X, 5 to Mark.X, 3 to Mark.O, 7 to Mark.O, 9 to Mark.O)
        assertEquals(8, TicTacToe.computerCell(twoWins, firstChoice))

        // X has 7 and 9, so the computer (which has no win of its own) must block at 8.
        val mustBlock = mapOf(1 to Mark.X, 3 to Mark.O, 5 to Mark.O, 7 to Mark.X, 9 to Mark.X)
        assertEquals(8, TicTacToe.computerCell(mustBlock, firstChoice))

        // Winning beats blocking: O wins at 2 (1-2-3) even though X threatens 4-5-6 at 6.
        val winBeatsBlock = mapOf(1 to Mark.O, 3 to Mark.O, 4 to Mark.X, 5 to Mark.X, 9 to Mark.X)
        assertEquals(2, TicTacToe.computerCell(winBeatsBlock, firstChoice))
    }

    @Test
    fun computerAlwaysPicksAFreeCell() {
        repeat(200) { seed ->
            val random = Random(seed)
            var state = TicTacToeState()
            while (!state.outcome.isOver) {
                val free = (1..9).filter { it !in state.board }
                val human = free[random.nextInt(free.size)]
                val next = TicTacToe.playAgainstComputer(state, human, random)
                assertEquals(state.moves.size + if (next.outcome.isOver && next.moves.size % 2 == 1) 1 else 2, next.moves.size)
                state = next
            }
        }
    }

    @Test
    fun computerAnswersImmediatelyUnlessTheGameEnded() {
        val afterOne = TicTacToe.playAgainstComputer(TicTacToeState(), 1, firstChoice)
        assertEquals(listOf(1 to Mark.X, 5 to Mark.O), afterOne.moves.map { it.cell to it.mark })
    }
}
