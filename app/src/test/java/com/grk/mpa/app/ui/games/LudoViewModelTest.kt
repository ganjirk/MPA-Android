
package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.Ludo
import com.grk.mpa.app.core.LudoKind
import com.grk.mpa.app.core.LudoPhase
import com.grk.mpa.app.core.LudoState
import com.grk.mpa.app.data.LudoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private class FakeLudoRepository(var stored: LudoState? = null) : LudoRepository {
    var saveCount = 0

    override suspend fun load(): LudoState? = stored

    override suspend fun save(state: LudoState) {
        saveCount++
        stored = state
    }
}

private class DelayedLudoRepository : LudoRepository {
    val loaded = CompletableDeferred<LudoState?>()
    var saveCount = 0

    override suspend fun load(): LudoState? = loaded.await()

    override suspend fun save(state: LudoState) {
        saveCount++
    }
}

class LudoViewModelTest {
    private fun viewModel(repo: FakeLudoRepository = FakeLudoRepository()) =
        LudoViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Random(1))

    @Test
    fun opensInSetupWithoutStartingOrRolling() {
        val vm = viewModel()

        assertTrue(vm.isLoaded)
        assertFalse(vm.gameStarted)
        assertEquals(listOf(LudoKind.HUMAN, LudoKind.CPU), vm.state.seats.map { it.kind })
        assertEquals(listOf(0, 2), vm.state.seats.map { it.color })
        assertEquals(0, vm.state.dice)
        assertEquals(LudoPhase.ROLL, vm.state.phase)
        assertFalse(vm.canRoll)
        assertFalse(vm.restored)
    }

    @Test
    fun setupChangesApplyWhenStartGameIsPressed() {
        val vm = viewModel()

        vm.onPlayerCountChange(3)
        vm.onKindChange(1, LudoKind.HUMAN)
        vm.onKindChange(2, LudoKind.EASY)

        assertEquals(2, vm.state.seats.size)
        vm.startGame()

        assertTrue(vm.gameStarted)
        assertEquals(
            listOf(LudoKind.HUMAN, LudoKind.HUMAN, LudoKind.EASY),
            vm.state.seats.map { it.kind }
        )
        assertEquals(0, vm.state.dice)
        assertEquals(LudoPhase.ROLL, vm.state.phase)

        vm.onPlayerCountChange(7)
        assertEquals(3, vm.playerCount)
    }

    @Test
    fun rollWithoutASixPassesTheTurnToTheComputer() {
        val vm = viewModel()
        vm.startGame()

        vm.roll(3)

        assertEquals(1, vm.state.turn)
        assertEquals(3, vm.state.dice)
        assertFalse(vm.canRoll)
    }

    @Test
    fun humanPicksATokenOnlyWhenThereIsAChoice() {
        val vm = viewModel()
        vm.startGame()

        vm.roll(6)
        assertTrue(vm.canPickToken)
        assertFalse(vm.autoMove())

        vm.onTokenClick(1, 0)
        assertEquals(LudoPhase.MOVE, vm.state.phase)

        vm.onTokenClick(0, 2)
        assertEquals(0, vm.state.pos[0][2])
        assertEquals(LudoPhase.ROLL, vm.state.phase)
        assertEquals(0, vm.state.turn)
    }

    @Test
    fun aSingleLegalMoveIsMadeForTheHuman() {
        val g = Ludo.newGame(listOf(LudoKind.HUMAN, LudoKind.CPU))
        val repo = FakeLudoRepository(
            g.copy(pos = listOf(listOf(5, -1, -1, -1), g.pos[1]))
        )
        val vm = viewModel(repo)

        assertTrue(vm.gameStarted)
        vm.roll(2)

        assertFalse(vm.canPickToken)
        assertTrue(vm.autoMove())
        assertEquals(7, vm.state.pos[0][0])
        assertEquals(1, vm.state.turn)
    }

    @Test
    fun computerMovesWithAutoMove() {
        val vm = viewModel()
        vm.startGame()

        vm.roll(3)
        assertFalse(vm.isHumanTurn)

        vm.roll(6)
        assertTrue(vm.autoMove())
        assertTrue(vm.state.pos[1].any { it == 0 })
    }

    @Test
    fun autoMoveDoesNothingBeforeGameStartsOrBeforeARoll() {
        val vm = viewModel()

        assertFalse(vm.autoMove())
        vm.roll(6)
        assertEquals(0, vm.state.dice)

        vm.startGame()
        assertFalse(vm.autoMove())
    }

    @Test
    fun progressIsSavedAndRestored() {
        val repo = FakeLudoRepository()
        val vm = viewModel(repo)

        vm.startGame()
        vm.roll(6)
        vm.onTokenClick(0, 0)

        assertTrue(repo.saveCount >= 2)
        assertNotNull(repo.stored)

        val again = viewModel(repo)
        assertTrue(again.restored)
        assertTrue(again.gameStarted)
        assertEquals(vm.state.pos, again.state.pos)
        assertEquals(2, again.playerCount)

        again.roll(4)
        assertFalse(again.restored)
    }

    @Test
    fun savedKindsComeBackAndSavedGameResumes() {
        val repo = FakeLudoRepository(
            Ludo.newGame(listOf(LudoKind.EASY, LudoKind.HUMAN, LudoKind.CPU))
        )
        val vm = viewModel(repo)

        assertEquals(3, vm.playerCount)
        assertEquals(
            listOf(LudoKind.EASY, LudoKind.HUMAN, LudoKind.CPU, LudoKind.CPU),
            vm.kinds
        )
        assertTrue(vm.gameStarted)
        assertTrue(vm.restored)
    }

    @Test
    fun newGameWaitsUntilSavedGameRestorationFinishes() {
        val repo = DelayedLudoRepository()
        val vm = LudoViewModel(
            repo,
            CoroutineScope(Dispatchers.Unconfined),
            Random(1)
        )

        assertFalse(vm.isLoaded)
        vm.newGame()
        vm.startGame()
        assertFalse(vm.isLoaded)
        assertEquals(0, repo.saveCount)

        val saved = Ludo.newGame(listOf(LudoKind.EASY, LudoKind.HUMAN))
        repo.loaded.complete(saved)

        assertTrue(vm.isLoaded)
        assertTrue(vm.restored)
        assertTrue(vm.gameStarted)
        assertEquals(saved.seats, vm.state.seats)
    }

    @Test
    fun newGameReturnsToSetupWithoutOverwritingSavedGame() {
        val repo = FakeLudoRepository()
        val vm = viewModel(repo)

        vm.startGame()
        vm.roll(6)
        val savedBeforeNewGame = repo.stored
        val savesBeforeNewGame = repo.saveCount

        vm.newGame()

        assertFalse(vm.gameStarted)
        assertFalse(vm.canRoll)
        assertEquals(savedBeforeNewGame, repo.stored)
        assertEquals(savesBeforeNewGame, repo.saveCount)

        vm.onPlayerCountChange(3)
        vm.onKindChange(1, LudoKind.HUMAN)
        vm.startGame()

        assertTrue(vm.gameStarted)
        assertEquals(3, vm.state.seats.size)
        assertEquals(
            listOf(LudoKind.HUMAN, LudoKind.HUMAN, LudoKind.CPU),
            vm.state.seats.map { it.kind }
        )
        assertEquals(0, vm.state.dice)
        assertEquals(3, repo.stored?.seats?.size)
    }
}
