package com.grk.mpa.app.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
