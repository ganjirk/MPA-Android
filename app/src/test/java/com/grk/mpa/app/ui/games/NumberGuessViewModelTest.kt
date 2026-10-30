package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.GuessLevel
import com.grk.mpa.app.core.GuessRecord
import com.grk.mpa.app.core.GuessSummary
import com.grk.mpa.app.core.NumberGuessGame
import com.grk.mpa.app.data.GuessStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeGuessStatsRepository(var stored: List<GuessRecord> = emptyList()) : GuessStatsRepository {
    override suspend fun load(): List<GuessRecord> = stored
    override suspend fun append(record: GuessRecord) {
        stored = stored + record
    }
}

class NumberGuessViewModelTest {
    private val repo = FakeGuessStatsRepository()

    private fun viewModel(target: Int = 25) = NumberGuessViewModel(
        statsRepository = repo,
        gameFactory = { level ->
            NumberGuessGame(range = 1..level.maxNumber, initialTarget = target, maxAttempts = level.chances)
        },
        coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        clock = { 1234L }
    )

    private fun NumberGuessViewModel.guess(value: Int) {
        onInputChange(value.toString())
        submit()
    }

    @Test
    fun startsOnTheNormalLevelWithFiveChances() {
        val vm = viewModel()
        assertEquals(GuessLevel.NORMAL, vm.level)
        assertEquals(50, vm.maxNumber)
        assertEquals(5, vm.chancesLeft)
        assertEquals(GuessFeedback.NONE, vm.feedback)
        assertFalse(vm.isFinished)
    }

    @Test
    fun inputKeepsOnlyDigitsAndLimitsLength() {
        val vm = viewModel()
        vm.onInputChange("a1b2-3.4567")
        assertEquals("1234", vm.input)
    }

    @Test
    fun emptyInputIsInvalidAndCostsNothing() {
        val vm = viewModel()
        vm.submit()
        assertEquals(GuessFeedback.INVALID, vm.feedback)
        assertEquals(5, vm.chancesLeft)
    }

    @Test
    fun outOfRangeGuessCostsNothingAndKeepsTheText() {
        val vm = viewModel()
        vm.guess(51)
        assertEquals(GuessFeedback.OUT_OF_RANGE, vm.feedback)
        assertEquals("51", vm.input)
        assertEquals(5, vm.chancesLeft)
        assertEquals(0, vm.attempts)
    }

    @Test
    fun hintsPointTowardsTheTargetAndUseUpChances() {
        val vm = viewModel(target = 25)
        vm.guess(10)
        assertEquals(GuessFeedback.TOO_LOW, vm.feedback)
        assertEquals(4, vm.chancesLeft)
        assertEquals("", vm.input)
        vm.guess(40)
        assertEquals(GuessFeedback.TOO_HIGH, vm.feedback)
        assertEquals(3, vm.chancesLeft)
        assertEquals(2, vm.attempts)
        assertFalse(vm.isFinished)
    }

    @Test
    fun lastChanceWarningAppearsWithOneChanceLeft() {
        val vm = viewModel()
        repeat(3) { vm.guess(1) }
        assertFalse(vm.isLastChance)
        vm.guess(1)
        assertEquals(1, vm.chancesLeft)
        assertTrue(vm.isLastChance)
    }

    @Test
    fun usingEveryChanceLosesRevealsTheNumberAndIsRecorded() {
        val vm = viewModel(target = 25)
        repeat(5) { vm.guess(1) }
        assertEquals(GuessFeedback.LOST, vm.feedback)
        assertTrue(vm.isFinished)
        assertEquals(25, vm.revealedTarget)
        assertFalse(vm.isLastChance)
        assertEquals(listOf(GuessRecord(GuessLevel.NORMAL, 5, won = false, playedAtMillis = 1234L)), repo.stored)
        assertEquals(1, vm.summary.total)
        assertEquals(0, vm.summary.wins)
    }

    @Test
    fun guessingTheNumberWinsAndIsRecorded() {
        val vm = viewModel(target = 25)
        vm.guess(10)
        vm.guess(25)
        assertEquals(GuessFeedback.CORRECT, vm.feedback)
        assertTrue(vm.isFinished)
        assertEquals(2, vm.attempts)
        assertEquals(25, vm.revealedTarget)
        assertEquals(listOf(GuessRecord(GuessLevel.NORMAL, 2, won = true, playedAtMillis = 1234L)), repo.stored)
        assertEquals(2, vm.summary.bestAttempts[GuessLevel.NORMAL])
        assertEquals(100, vm.summary.winRatePercent)
    }

    @Test
    fun guessesAfterTheGameEndedAreIgnored() {
        val vm = viewModel(target = 25)
        vm.guess(25)
        vm.guess(3)
        assertEquals(GuessFeedback.CORRECT, vm.feedback)
        assertEquals(1, vm.attempts)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun changingTheLevelStartsANewGame() {
        val vm = viewModel()
        vm.guess(1)
        vm.onLevelChange(GuessLevel.HARD)
        assertEquals(GuessLevel.HARD, vm.level)
        assertEquals(200, vm.maxNumber)
        assertEquals(5, vm.chancesLeft)
        assertEquals(0, vm.attempts)
        assertEquals(GuessFeedback.NONE, vm.feedback)
        assertNull(vm.revealedTarget)
    }

    @Test
    fun newGameKeepsTheLevelAndTheStats() {
        val vm = viewModel(target = 25)
        vm.onLevelChange(GuessLevel.MEDIUM)
        vm.guess(25)
        vm.newGame()
        assertEquals(GuessLevel.MEDIUM, vm.level)
        assertFalse(vm.isFinished)
        assertEquals(5, vm.chancesLeft)
        assertEquals(1, vm.summary.total)
        assertEquals(GuessLevel.MEDIUM, repo.stored.single().level)
    }

    @Test
    fun existingStatsAreLoadedOnStart() {
        repo.stored = listOf(
            GuessRecord(GuessLevel.NORMAL, 3, true, 1),
            GuessRecord(GuessLevel.NORMAL, 5, false, 2)
        )
        val vm = viewModel()
        assertEquals(2, vm.summary.total)
        assertEquals(50, vm.summary.winRatePercent)
        assertEquals(GuessSummary(2, 1, mapOf(GuessLevel.NORMAL to 3)), vm.summary)
    }
}
