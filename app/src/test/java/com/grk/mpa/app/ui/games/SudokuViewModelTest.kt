package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.SudokuDifficulty
import com.grk.mpa.app.core.SudokuStatus
import com.grk.mpa.app.core.SudokuStore
import com.grk.mpa.app.data.SudokuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private class FakeSudokuRepository(var stored: SudokuStore = SudokuStore()) : SudokuRepository {
    override suspend fun load(): SudokuStore = stored
    override suspend fun save(store: SudokuStore) {
        stored = store
    }
}

class SudokuViewModelTest {
    private var now = 0L

    private fun viewModel(repo: FakeSudokuRepository = FakeSudokuRepository()) =
        SudokuViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Random(7)) { now }

    private fun SudokuViewModel.solveEverything(): Unit {
        val g = game!!
        g.puzzle.indices.filter { g.puzzle[it] == 0 }.forEach { i ->
            select(i)
            enter(g.solution[i])
        }
    }

    @Test
    fun startsWithAMediumPuzzleAndSavesIt() {
        val repo = FakeSudokuRepository()
        val vm = viewModel(repo)
        assertEquals(SudokuDifficulty.MEDIUM, vm.difficulty)
        assertNotNull(vm.game)
        assertNotNull(repo.stored.games[SudokuDifficulty.MEDIUM])
    }

    @Test
    fun resumesTheSavedGame() {
        val repo = FakeSudokuRepository()
        val first = viewModel(repo)
        val g = first.game!!
        val cell = g.puzzle.indexOf(0)
        first.select(cell)
        first.enter(g.solution[cell])
        first.onStop()

        val second = viewModel(repo)
        assertEquals(g.puzzle, second.game!!.puzzle)
        assertEquals(g.solution[cell], second.game!!.values[cell])
    }

    @Test
    fun switchingDifficultyKeepsEachGame() {
        val repo = FakeSudokuRepository()
        val vm = viewModel(repo)
        val medium = vm.game!!.puzzle
        vm.onDifficultyChange(SudokuDifficulty.EASY)
        assertEquals(SudokuDifficulty.EASY, vm.difficulty)
        vm.onDifficultyChange(SudokuDifficulty.MEDIUM)
        assertEquals(medium, vm.game!!.puzzle)
    }

    @Test
    fun newPuzzleReplacesTheGame() {
        val vm = viewModel()
        val before = vm.game!!.puzzle
        vm.newPuzzle()
        assertFalse(before == vm.game!!.puzzle)
    }

    @Test
    fun finishingRecordsABestTimeAndClearsTheSave() {
        val repo = FakeSudokuRepository()
        val vm = viewModel(repo)
        vm.onStart()
        now = 7_000
        vm.solveEverything()
        val status = vm.status
        assertTrue(status is SudokuStatus.Completed)
        assertEquals(7_000L, vm.bestMs[SudokuDifficulty.MEDIUM])
        assertTrue(vm.game!!.solved)
        assertNull(repo.stored.games[SudokuDifficulty.MEDIUM])
        assertEquals(7_000L, repo.stored.bestMs[SudokuDifficulty.MEDIUM])
    }

    @Test
    fun hintsDisqualifyABestTime() {
        val vm = viewModel()
        vm.hint()
        vm.solveEverything()
        assertTrue(vm.game!!.solved)
        assertTrue(vm.bestMs.isEmpty())
    }

    @Test
    fun clockRunsOnlyWhileTheScreenIsShowing() {
        val vm = viewModel()
        vm.onStart()
        now = 5_000
        vm.refreshTime()
        assertEquals(5_000L, vm.elapsedMs)
        vm.onStop()
        now = 9_000
        vm.refreshTime()
        assertEquals(5_000L, vm.elapsedMs)
    }
}
