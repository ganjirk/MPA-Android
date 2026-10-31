package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.GameRecord
import com.grk.mpa.app.core.Mark
import com.grk.mpa.app.core.MatchWinner
import com.grk.mpa.app.core.NameError
import com.grk.mpa.app.core.TicTacToeStats
import com.grk.mpa.app.data.TicTacToeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private class FakeTicTacToeRepository(var stored: TicTacToeStats = TicTacToeStats()) : TicTacToeRepository {
    var saveCount = 0
    override suspend fun load(): TicTacToeStats = stored
    override suspend fun save(stats: TicTacToeStats) {
        saveCount++
        stored = stats
    }
}

class TicTacToeViewModelTest {
    private fun viewModel(repo: FakeTicTacToeRepository = FakeTicTacToeRepository()) =
        TicTacToeViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Random(1)) { 500L }

    @Test
    fun loadsSavedStats() {
        val repo = FakeTicTacToeRepository(TicTacToeStats(computer = GameRecord(2, 1, 1, 0)))
        val vm = viewModel(repo)
        assertTrue(vm.isLoaded)
        assertEquals(2, vm.stats.computer.games)
    }

    @Test
    fun computerAnswersEachMove() {
        val vm = viewModel()
        vm.onCellClick(1)
        assertEquals(2, vm.state.moves.size)
        assertEquals(Mark.X, vm.state.moves[0].mark)
        assertEquals(5, vm.state.moves[1].cell) // the centre is free, so the computer takes it
        assertEquals(Mark.O, vm.state.moves[1].mark)
    }

    @Test
    fun takenCellsAreIgnored() {
        val vm = viewModel()
        vm.onCellClick(1)
        val before = vm.state
        vm.onCellClick(1)
        vm.onCellClick(5)
        assertEquals(before, vm.state)
    }

    @Test
    fun losingToTheComputerIsRecordedOnceAndSaved() {
        val repo = FakeTicTacToeRepository()
        val vm = viewModel(repo)
        // X: 1, 2, 4 against the computer's 5, 3 (block), 7 (win).
        vm.onCellClick(1)
        vm.onCellClick(2)
        vm.onCellClick(4)
        assertEquals(Mark.O, vm.outcome.winner)
        assertEquals(GameRecord(games = 1, wins = 0, losses = 1, draws = 0), vm.stats.computer)
        assertEquals(vm.stats, repo.stored)

        vm.onCellClick(6) // the game is over: nothing changes
        assertEquals(1, vm.stats.computer.games)
        assertEquals(1, repo.saveCount)
    }

    @Test
    fun newGameClearsTheBoardButKeepsStats() {
        val vm = viewModel()
        vm.onCellClick(1)
        vm.newGame()
        assertTrue(vm.state.moves.isEmpty())
        vm.onCellClick(1)
        vm.onCellClick(2)
        vm.onCellClick(4)
        vm.newGame()
        assertTrue(vm.state.moves.isEmpty())
        assertEquals(1, vm.stats.computer.games)
    }

    @Test
    fun abandonedGamesAreNotCounted() {
        val vm = viewModel()
        vm.onCellClick(1)
        vm.newGame()
        assertEquals(0, vm.stats.computer.games)
    }

    @Test
    fun switchingModeStartsAFreshGame() {
        val vm = viewModel()
        vm.onCellClick(1)
        vm.onModeChange(TicTacToeMode.LOCAL)
        assertEquals(TicTacToeMode.LOCAL, vm.mode)
        assertTrue(vm.state.moves.isEmpty())
    }

    @Test
    fun twoPlayersTakeTurnsAndTheWinnerIsRecorded() {
        val repo = FakeTicTacToeRepository()
        val vm = viewModel(repo)
        vm.onModeChange(TicTacToeMode.LOCAL)
        vm.onNameChange(Mark.X, "Asha")
        vm.onNameChange(Mark.O, "Ravi")
        listOf(1, 4, 2, 5, 3).forEach(vm::onCellClick)

        assertEquals(Mark.X, vm.outcome.winner)
        assertEquals(listOf(1, 2, 3), vm.outcome.line)
        assertEquals(0, vm.stats.computer.games)
        val asha = vm.stats.players.first { it.normalizedName == "asha" }
        val ravi = vm.stats.players.first { it.normalizedName == "ravi" }
        assertEquals(1, asha.record.wins)
        assertEquals(1, ravi.record.losses)
        assertEquals(MatchWinner.X, vm.stats.recentMatches.single().winner)
        assertEquals(500L, vm.stats.recentMatches.single().playedAtMillis)
        assertEquals(vm.stats, repo.stored)
    }

    @Test
    fun twoPlayerGameCanEndInADraw() {
        val vm = viewModel()
        vm.onModeChange(TicTacToeMode.LOCAL)
        // X O X / X O O / O X X  has no winning line.
        listOf(1, 2, 3, 5, 4, 6, 8, 7, 9).forEach(vm::onCellClick)
        assertTrue(vm.outcome.isDraw)
        assertEquals(1, vm.stats.players.first().record.draws)
    }

    @Test
    fun sameOrEmptyNamesBlockTheFirstMove() {
        val vm = viewModel()
        vm.onModeChange(TicTacToeMode.LOCAL)
        vm.onNameChange(Mark.O, "player 1")
        vm.onCellClick(1)
        assertEquals(NameError.SAME, vm.nameError)
        assertTrue(vm.state.moves.isEmpty())

        vm.onNameChange(Mark.O, "")
        vm.onCellClick(1)
        assertEquals(NameError.LENGTH, vm.nameError)

        vm.onNameChange(Mark.O, "Ravi")
        vm.onCellClick(1)
        assertEquals(null, vm.nameError)
        assertEquals(1, vm.state.moves.size)
    }

    @Test
    fun namesAreLockedOnceTheGameHasStarted() {
        val vm = viewModel()
        vm.onModeChange(TicTacToeMode.LOCAL)
        vm.onCellClick(1)
        assertFalse(vm.canEditNames)
        vm.onNameChange(Mark.X, "Changed")
        assertEquals("Player 1", vm.playerXName)
    }

    @Test
    fun nameLengthIsLimited() {
        val vm = viewModel()
        vm.onModeChange(TicTacToeMode.LOCAL)
        vm.onNameChange(Mark.X, "x".repeat(60))
        assertEquals(24, vm.playerXName.length)
    }
}
