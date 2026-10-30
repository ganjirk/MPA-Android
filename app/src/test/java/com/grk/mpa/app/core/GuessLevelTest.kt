package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GuessLevelTest {
    @Test
    fun levelsMatchTheWebsite() {
        assertEquals(listOf(50, 100, 200), GuessLevel.entries.map { it.maxNumber })
        assertEquals(listOf("Normal", "Medium", "Hard"), GuessLevel.entries.map { it.label })
        assertEquals(listOf(5, 5, 5), GuessLevel.entries.map { it.chances })
        assertEquals(GuessLevel.NORMAL, GuessLevel.DEFAULT)
    }

    @Test
    fun newGameUsesTheLevelsRangeAndChances() {
        GuessLevel.entries.forEach { level ->
            val game = level.newGame()
            assertEquals(1..level.maxNumber, game.range)
            assertEquals(level.chances, game.maxAttempts)
            assertEquals(level.chances, game.attemptsLeft)
        }
    }
}
