package com.grk.mpa.app.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberGuessGameTest {
    @Test
    fun hintsPointTowardsTheTarget() {
        val game = NumberGuessGame(initialTarget = 25)
        assertEquals(GuessResult.TOO_LOW, game.guess(10))
        assertEquals(GuessResult.TOO_HIGH, game.guess(40))
        assertEquals(GuessResult.CORRECT, game.guess(25))
        assertTrue(game.isSolved)
        assertEquals(3, game.attempts)
    }

    @Test
    fun outOfRangeGuessesDoNotCountAsAttempts() {
        val game = NumberGuessGame(initialTarget = 25)
        assertEquals(GuessResult.OUT_OF_RANGE, game.guess(0))
        assertEquals(GuessResult.OUT_OF_RANGE, game.guess(51))
        assertEquals(0, game.attempts)
    }

    @Test
    fun resetStartsANewRound() {
        val game = NumberGuessGame(initialTarget = 25, random = Random(1))
        game.guess(25)
        game.reset()
        assertFalse(game.isSolved)
        assertEquals(0, game.attempts)
    }

    @Test
    fun resetPicksAValueInsideTheRange() {
        val game = NumberGuessGame(range = 1..3, random = Random(7))
        repeat(50) {
            game.reset()
            // The target is always inside 1..3, so guessing every value must hit it.
            (1..3).forEach { game.guess(it) }
            assertTrue(game.isSolved)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun targetOutsideRangeIsRejected() {
        NumberGuessGame(initialTarget = 99)
    }

    @Test
    fun runningOutOfAttemptsLosesAndRevealsTheNumber() {
        val game = NumberGuessGame(initialTarget = 25, maxAttempts = 3)
        assertNull(game.revealedTarget)
        assertEquals(GuessResult.TOO_LOW, game.guess(1))
        assertEquals(2, game.attemptsLeft)
        assertEquals(GuessResult.TOO_LOW, game.guess(2))
        assertEquals(GuessResult.LOST, game.guess(3))
        assertTrue(game.isLost)
        assertFalse(game.isSolved)
        assertEquals(0, game.attemptsLeft)
        assertEquals(25, game.revealedTarget)
    }

    @Test
    fun winningOnTheLastAttemptIsStillAWin() {
        val game = NumberGuessGame(initialTarget = 25, maxAttempts = 2)
        game.guess(1)
        assertEquals(GuessResult.CORRECT, game.guess(25))
        assertTrue(game.isSolved)
        assertFalse(game.isLost)
    }

    @Test
    fun guessesAfterTheGameEndedRepeatTheFinalResult() {
        val lost = NumberGuessGame(initialTarget = 25, maxAttempts = 1)
        assertEquals(GuessResult.LOST, lost.guess(1))
        assertEquals(GuessResult.LOST, lost.guess(25))
        assertEquals(1, lost.attempts)

        val won = NumberGuessGame(initialTarget = 25, maxAttempts = 3)
        won.guess(25)
        assertEquals(GuessResult.CORRECT, won.guess(1))
        assertEquals(1, won.attempts)
    }

    @Test
    fun outOfRangeGuessesDoNotUseUpAChance() {
        val game = NumberGuessGame(initialTarget = 25, maxAttempts = 2)
        game.guess(0)
        game.guess(99)
        assertEquals(2, game.attemptsLeft)
        assertFalse(game.isLost)
    }

    @Test
    fun unlimitedGamesHaveNoAttemptsLeftValueAndNeverLose() {
        val game = NumberGuessGame(initialTarget = 25)
        assertNull(game.attemptsLeft)
        repeat(100) { game.guess(1) }
        assertFalse(game.isLost)
    }

    @Test
    fun resetClearsALostGame() {
        val game = NumberGuessGame(initialTarget = 25, maxAttempts = 1, random = Random(3))
        game.guess(1)
        assertTrue(game.isLost)
        game.reset()
        assertFalse(game.isLost)
        assertEquals(1, game.attemptsLeft)
        assertNull(game.revealedTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxAttemptsMustBePositive() {
        NumberGuessGame(maxAttempts = 0)
    }
}
