package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.Ludo
import com.grk.mpa.app.core.LudoKind
import com.grk.mpa.app.core.LudoPhase
import com.grk.mpa.app.core.LudoState
import com.grk.mpa.app.data.LudoRepository
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

class LudoViewModelTest {
    private fun viewModel(repo: FakeLudoRepository = FakeLudoRepository()) =
        LudoViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Random(1))

    @Test
    fun startsWithAHumanAgainstTheComputer() {
        val vm = viewModel()
        assertTrue(vm.isLoaded)
        assertEquals(listOf(LudoKind.HUMAN, LudoKind.CPU), vm.state.seats.map { it.kind })
        assertEquals(listOf(0, 2), vm.state.seats.map { it.color })
        assertTrue(vm.canRoll)
        assertFalse(vm.restored)
    }

    @Test
    fun setupChangesApplyOnNewGame() {
        val vm = viewModel()
        vm.onPlayerCountChange(3)
        vm.onKindChange(1, LudoKind.HUMAN)
        vm.onKindChange(2, LudoKind.EASY)
        assertEquals(2, vm.state.seats.size)
        vm.newGame()
        assertEquals(listOf(LudoKind.HUMAN, LudoKind.HUMAN, LudoKind.EASY), vm.state.seats.map { it.kind })
        vm.onPlayerCountChange(7) // ignored
        assertEquals(3, vm.playerCount)
    }

    @Test
    fun rollWithoutASixPassesTheTurnToTheComputer() {
        val vm = viewModel()
        vm.roll(3)
        assertEquals(1, vm.state.turn)
        assertEquals(3, vm.state.dice)
        assertFalse(vm.canRoll)
    }

    @Test
    fun humanPicksATokenOnlyWhenThereIsAChoice() {
        val vm = viewModel()
        vm.roll(6)
        assertTrue(vm.canPickToken)
        assertFalse(vm.autoMove()) // a human decides
        vm.onTokenClick(1, 0) // not their seat
        assertEquals(LudoPhase.MOVE, vm.state.phase)
        vm.onTokenClick(0, 2)
        assertEquals(0, vm.state.pos[0][2])
        assertEquals(LudoPhase.ROLL, vm.state.phase)
        assertEquals(0, vm.state.turn) // a six earns another roll
    }

    @Test
    fun aSingleLegalMoveIsMadeForTheHuman() {
        val g = Ludo.newGame(listOf(LudoKind.HUMAN, LudoKind.CPU))
        val vm = viewModel(FakeLudoRepository(g.copy(pos = listOf(listOf(5, -1, -1, -1), g.pos[1]))))
        vm.roll(2)
        assertFalse(vm.canPickToken)
        assertTrue(vm.autoMove())
        assertEquals(7, vm.state.pos[0][0])
        assertEquals(1, vm.state.turn)
    }

    @Test
    fun computerMovesWithAutoMove() {
        val vm = viewModel()
        vm.roll(3) // red cannot move, yellow's turn
        assertFalse(vm.isHumanTurn)
        vm.roll(6)
        assertTrue(vm.autoMove())
        assertTrue(vm.state.pos[1].any { it == 0 })
    }

    @Test
    fun autoMoveDoesNothingBeforeARoll() {
        val vm = viewModel()
        assertFalse(vm.autoMove())
    }

    @Test
    fun progressIsSavedAndRestored() {
        val repo = FakeLudoRepository()
        val vm = viewModel(repo)
        vm.roll(6)
        vm.onTokenClick(0, 0)
        assertTrue(repo.saveCount >= 2)
        assertNotNull(repo.stored)

        val again = viewModel(repo)
        assertTrue(again.restored)
        assertEquals(vm.state.pos, again.state.pos)
        assertEquals(2, again.playerCount)
        again.roll(4)
        assertFalse(again.restored)
    }

    @Test
    fun savedKindsComeBackInTheSetup() {
        val repo = FakeLudoRepository(Ludo.newGame(listOf(LudoKind.EASY, LudoKind.HUMAN, LudoKind.CPU)))
        val vm = viewModel(repo)
        assertEquals(3, vm.playerCount)
        assertEquals(listOf(LudoKind.EASY, LudoKind.HUMAN, LudoKind.CPU, LudoKind.CPU), vm.kinds)
    }

    @Test
    fun newGameReplacesTheSavedGame() {
        val repo = FakeLudoRepository()
        val vm = viewModel(repo)
        vm.roll(6)
        vm.newGame()
        assertEquals(List(4) { -1 }, vm.state.pos[0])
        assertEquals(vm.state.copy(events = emptyList()), repo.stored?.copy(events = emptyList()))
    }
}
