package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.MatchDifficulty
import com.grk.mpa.app.core.MatchResult
import com.grk.mpa.app.data.MatchCardsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private class FakeMatchCardsRepository(var stored: List<MatchResult> = emptyList()) : MatchCardsRepository {
    override suspend fun load() = stored
    override suspend fun save(results: List<MatchResult>) {
        stored = results
    }
}

class MatchCardsViewModelTest {
    private var now = 1_000_000L

    private fun viewModel(repo: FakeMatchCardsRepository = FakeMatchCardsRepository()) =
        MatchCardsViewModel(repo, CoroutineScope(Dispatchers.Unconfined), Random(5)) { now }

    private fun MatchCardsViewModel.partnerOf(i: Int) =
        game.cards.indices.first { it != i && game.cards[it].symbol == game.cards[i].symbol }

    private fun MatchCardsViewModel.playPerfectly() {
        for (i in game.cards.indices) {
            if (game.cards[i].matched) continue
            onCardClick(i)
            onCardClick(partnerOf(i))
        }
    }

    @Test
    fun startsEasyAndReady() {
        val vm = viewModel()
        assertEquals(MatchDifficulty.EASY, vm.difficulty)
        assertEquals(12, vm.game.cards.size)
        assertEquals(MatchStatus.PROMPT, vm.status)
        assertEquals(0, vm.elapsedSeconds)
    }

    @Test
    fun matchAndMissChangeTheStatus() {
        val vm = viewModel()
        vm.onCardClick(0)
        vm.onCardClick(vm.partnerOf(0))
        assertEquals(MatchStatus.MATCH, vm.status)

        val next = vm.game.cards.indices.first { !vm.game.cards[it].matched }
        val other = vm.game.cards.indices.first { !vm.game.cards[it].matched && vm.game.cards[it].symbol != vm.game.cards[next].symbol }
        vm.onCardClick(next)
        vm.onCardClick(other)
        assertEquals(MatchStatus.NO_MATCH, vm.status)
        assertTrue(vm.game.mismatchPending)
        vm.hideMismatch()
        assertFalse(vm.game.mismatchPending)
    }

    @Test
    fun clockStartsOnTheFirstFlip() {
        val vm = viewModel()
        now += 5_000
        vm.tick()
        assertEquals(0, vm.elapsedSeconds) // nothing flipped yet
        vm.onCardClick(0)
        now += 7_400
        vm.tick()
        assertEquals(7, vm.elapsedSeconds)
    }

    @Test
    fun finishingSavesTheResult() {
        val repo = FakeMatchCardsRepository()
        val vm = viewModel(repo)
        vm.onCardClick(0)
        now += 42_900
        vm.playPerfectly()
        assertEquals(MatchStatus.DONE, vm.status)
        assertEquals(42, vm.elapsedSeconds)
        val saved = repo.stored.single()
        assertEquals(MatchDifficulty.EASY, saved.difficulty)
        assertEquals(6, saved.moves)
        assertEquals(42, saved.elapsedSeconds)
        assertEquals(saved, vm.history.single())
        vm.tick() // the clock stays put once finished
        assertEquals(42, vm.elapsedSeconds)
    }

    @Test
    fun aVeryFastGameStillTakesAtLeastOneSecond() {
        val vm = viewModel()
        vm.playPerfectly()
        assertEquals(1, vm.elapsedSeconds)
    }

    @Test
    fun changingDifficultyOrRestartingStartsOver() {
        val vm = viewModel()
        vm.onCardClick(0)
        vm.onDifficultyChange(MatchDifficulty.HARD)
        assertEquals(24, vm.game.cards.size)
        assertEquals(0, vm.game.moves)
        assertEquals(-1, vm.game.first)
        vm.onCardClick(0)
        vm.newGame()
        assertEquals(-1, vm.game.first)
        assertEquals(MatchStatus.PROMPT, vm.status)
    }

    @Test
    fun savedHistoryIsLoaded() {
        val old = MatchResult(5L, MatchDifficulty.MEDIUM, 12, 40)
        val vm = viewModel(FakeMatchCardsRepository(listOf(old)))
        assertEquals(listOf(old), vm.history)
    }
}
