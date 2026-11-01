package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.ChessLevel
import com.grk.mpa.app.core.ChessMode
import com.grk.mpa.app.core.ChessMoveRef
import com.grk.mpa.app.core.ChessResult
import com.grk.mpa.app.core.ChessSave
import com.grk.mpa.app.data.ChessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private class FakeChessRepository(var stored: ChessSave? = null) : ChessRepository {
    override suspend fun load(): ChessSave? = stored
    override suspend fun save(save: ChessSave) {
        stored = save
    }
}

class ChessViewModelTest {
    private fun sq(name: String): Int = (8 - (name[1] - '0')) * 8 + "abcdefgh".indexOf(name[0])

    private fun viewModel(repo: FakeChessRepository = FakeChessRepository()) =
        ChessViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Random(1)).also {
            it.onLevelChange(ChessLevel.EASY)
        }

    private fun ChessViewModel.tap(from: String, to: String) {
        onSquare(sq(from))
        onSquare(sq(to))
    }

    @Test
    fun startsAsWhiteAgainstTheComputer() {
        val vm = viewModel()
        assertTrue(vm.isLoaded)
        assertTrue(vm.vsComputer)
        assertEquals(1, vm.side)
        assertEquals(20, vm.legalMoves.size)
        assertTrue(vm.canMove)
        assertFalse(vm.flipped)
    }

    @Test
    fun humanMoveIsAnsweredByTheComputer() {
        val vm = viewModel()
        vm.tap("e2", "e4")
        assertEquals(2, vm.history.size)
        assertEquals("e4", vm.history[0].san)
        assertEquals(1, vm.position.turn)
        assertTrue(vm.canMove)
    }

    @Test
    fun onlyYourOwnMovablePiecesCanBeSelected() {
        val vm = viewModel()
        vm.onSquare(sq("e7")) // black's pawn
        assertEquals(-1, vm.selected)
        vm.onSquare(sq("e4")) // empty
        assertEquals(-1, vm.selected)
        vm.onSquare(sq("a1")) // a rook with no legal move
        assertEquals(-1, vm.selected)
        vm.onSquare(sq("e2"))
        assertEquals(sq("e2"), vm.selected)
        vm.onSquare(sq("e2"))
        assertEquals(-1, vm.selected) // tapping again deselects
    }

    @Test
    fun illegalTargetKeepsNothingSelectedOrMoved() {
        val vm = viewModel()
        vm.tap("e2", "e5")
        assertEquals(0, vm.history.size)
    }

    @Test
    fun playingBlackLetsTheComputerOpenAndFlipsTheBoard() {
        val vm = viewModel()
        vm.onSideChange(-1)
        assertEquals(-1, vm.side)
        assertTrue(vm.flipped)
        assertEquals(1, vm.history.size)
        assertEquals(-1, vm.position.turn)
    }

    @Test
    fun twoPlayersAlternateWithoutTheComputer() {
        val vm = viewModel()
        vm.onModeChange(ChessMode.TWO_PLAYERS)
        vm.tap("e2", "e4")
        assertEquals(1, vm.history.size)
        assertEquals(-1, vm.position.turn)
        vm.tap("e7", "e5")
        assertEquals(2, vm.history.size)
    }

    @Test
    fun undoGoesBackToYourLastTurnAgainstTheComputer() {
        val vm = viewModel()
        assertFalse(vm.canUndo)
        vm.tap("e2", "e4")
        assertTrue(vm.canUndo)
        vm.undo()
        assertEquals(0, vm.history.size)
        assertEquals(1, vm.position.turn)
        assertFalse(vm.canUndo)
    }

    @Test
    fun undoStepsBackOneMoveInTwoPlayerGames() {
        val vm = viewModel()
        vm.onModeChange(ChessMode.TWO_PLAYERS)
        vm.tap("e2", "e4")
        vm.tap("e7", "e5")
        vm.undo()
        assertEquals(1, vm.history.size)
        assertEquals(-1, vm.position.turn)
    }

    @Test
    fun resigningEndsTheGame() {
        val vm = viewModel()
        assertFalse(vm.canResign)
        vm.tap("e2", "e4")
        assertTrue(vm.canResign)
        vm.resign()
        val outcome = vm.outcome
        assertNotNull(outcome)
        assertEquals(ChessResult.RESIGN, outcome!!.result)
        assertEquals(-1, outcome.winner) // you were white and resigned
        assertFalse(vm.canMove)
        vm.undo() // a resigned game can be taken back
        assertEquals(null, vm.outcome)
    }

    @Test
    fun checkmateEndsTheGame() {
        val vm = viewModel()
        vm.onModeChange(ChessMode.TWO_PLAYERS)
        vm.tap("f2", "f3"); vm.tap("e7", "e5"); vm.tap("g2", "g4"); vm.tap("d8", "h4")
        assertEquals(ChessResult.CHECKMATE, vm.outcome?.result)
        assertEquals(-1, vm.outcome?.winner)
        assertTrue(vm.inCheck)
        assertFalse(vm.canMove)
    }

    @Test
    fun promotionAsksWhichPiece() {
        val repo = FakeChessRepository()
        val vm = viewModel(repo)
        vm.onModeChange(ChessMode.TWO_PLAYERS)
        // White's a-pawn marches and captures its way to the eighth rank: 1.a4 b5 2.axb5 a6 3.bxa6 Nf6 4.a7 h6 5.axb8
        listOf("a2a4", "b7b5", "a4b5", "a7a6", "b5a6", "g8f6", "a6a7", "h7h6").forEach { vm.tap(it.take(2), it.drop(2)) }
        vm.tap("a7", "b8")
        assertNotNull(vm.pendingPromotion)
        assertFalse(vm.canMove)
        vm.onPromotion(5)
        assertEquals(null, vm.pendingPromotion)
        assertEquals("axb8=Q", vm.history.last().san)
    }

    @Test
    fun gameIsSavedAndRestored() {
        val repo = FakeChessRepository()
        val vm = viewModel(repo)
        vm.tap("e2", "e4")
        vm.onSideChange(1) // no change, nothing restarts
        assertEquals(2, vm.history.size)

        val again = ChessViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Random(1))
        assertEquals(2, again.history.size)
        assertEquals(vm.position.turn, again.position.turn)
        assertEquals(ChessLevel.EASY, again.level)
    }

    @Test
    fun damagedSaveStartsANewGame() {
        val repo = FakeChessRepository(
            ChessSave(ChessMode.TWO_PLAYERS, 1, ChessLevel.MEDIUM, false, 0, listOf(ChessMoveRef(52, 20)))
        )
        val vm = ChessViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Random(1))
        assertTrue(vm.isLoaded)
        assertEquals(0, vm.history.size)
        assertEquals(20, vm.legalMoves.size)
    }

    @Test
    fun newGameClearsTheBoard() {
        val vm = viewModel()
        vm.tap("e2", "e4")
        vm.newGame()
        assertEquals(0, vm.history.size)
        assertEquals(null, vm.outcome)
    }
}
