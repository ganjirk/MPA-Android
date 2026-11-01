package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchCardsTest {
    private fun game(difficulty: MatchDifficulty = MatchDifficulty.EASY) = MatchCards.newGame(difficulty, Random(3))

    /** Positions of the two cards with the same symbol as card 0, and one card with a different symbol. */
    private fun MatchGame.partnerOf(i: Int) = cards.indices.first { it != i && cards[it].symbol == cards[i].symbol }
    private fun MatchGame.otherThan(i: Int) = cards.indices.first { cards[it].symbol != cards[i].symbol }

    @Test
    fun deckHasEachSymbolTwice() {
        MatchDifficulty.entries.forEach { level ->
            val g = game(level)
            assertEquals(level.pairs * 2, g.cards.size)
            val counts = g.cards.groupingBy { it.symbol }.eachCount()
            assertEquals(level.pairs, counts.size)
            assertTrue(counts.values.all { it == 2 })
            assertEquals(g.cards.indices.toList(), g.cards.map { it.id })
        }
        assertEquals(listOf(6, 8, 12), MatchDifficulty.entries.map { it.pairs })
    }

    @Test
    fun shufflingChangesTheOrder() {
        assertTrue(MatchCards.newGame(MatchDifficulty.HARD, Random(1)).cards.map { it.symbol } !=
            MatchCards.newGame(MatchDifficulty.HARD, Random(2)).cards.map { it.symbol })
    }

    @Test
    fun firstFlipIsNotAMove() {
        val g = MatchCards.flip(game(), 0)
        assertEquals(0, g.first)
        assertEquals(0, g.moves)
        assertTrue(g.isRevealed(0))
    }

    @Test
    fun flippingTheSameCardTwiceDoesNothing() {
        val g = MatchCards.flip(game(), 0)
        assertSame(g, MatchCards.flip(g, 0))
    }

    @Test
    fun matchingPairStaysUp() {
        val start = game()
        val partner = start.partnerOf(0)
        val g = MatchCards.flip(MatchCards.flip(start, 0), partner)
        assertEquals(1, g.moves)
        assertEquals(1, g.matches)
        assertTrue(g.cards[0].matched && g.cards[partner].matched)
        assertEquals(-1, g.first)
        assertFalse(g.mismatchPending)
        assertTrue(g.isRevealed(0))
    }

    @Test
    fun missLocksTheBoardUntilTheyAreHidden() {
        val start = game()
        val other = start.otherThan(0)
        val miss = MatchCards.flip(MatchCards.flip(start, 0), other)
        assertEquals(1, miss.moves)
        assertTrue(miss.mismatchPending)
        assertTrue(miss.isRevealed(0) && miss.isRevealed(other))
        val third = miss.cards.indices.first { it != 0 && it != other }
        assertSame(miss, MatchCards.flip(miss, third)) // locked

        val hidden = MatchCards.hideMismatch(miss)
        assertFalse(hidden.mismatchPending)
        assertFalse(hidden.isRevealed(0))
        assertFalse(hidden.isRevealed(other))
        assertEquals(1, hidden.moves)
        assertSame(hidden, MatchCards.hideMismatch(hidden))
    }

    @Test
    fun matchedCardsCannotBeFlippedAgain() {
        val start = game()
        val partner = start.partnerOf(0)
        val g = MatchCards.flip(MatchCards.flip(start, 0), partner)
        assertSame(g, MatchCards.flip(g, 0))
        assertSame(g, MatchCards.flip(g, 99))
        assertSame(g, MatchCards.flip(g, -1))
    }

    @Test
    fun matchingEverythingFinishesTheGame() {
        var g = game()
        for (i in g.cards.indices) {
            if (g.cards[i].matched) continue
            g = MatchCards.flip(MatchCards.flip(g, i), g.partnerOf(i))
        }
        assertTrue(g.finished)
        assertEquals(6, g.matches)
        assertEquals(6, g.moves) // a perfect game: one move per pair
        assertSame(g, MatchCards.flip(g, 0))
    }

    // ---- history ----

    private fun result(d: MatchDifficulty, moves: Int, seconds: Int, at: Long = 0) = MatchResult(at, d, moves, seconds)

    @Test
    fun bestIsFewestMovesThenFastest() {
        val list = listOf(
            result(MatchDifficulty.EASY, 10, 50), result(MatchDifficulty.EASY, 8, 70),
            result(MatchDifficulty.EASY, 8, 40), result(MatchDifficulty.HARD, 14, 90)
        )
        assertEquals(result(MatchDifficulty.EASY, 8, 40), MatchHistory.best(list, MatchDifficulty.EASY))
        assertEquals(14, MatchHistory.best(list, MatchDifficulty.HARD)?.moves)
        assertNull(MatchHistory.best(list, MatchDifficulty.MEDIUM))
    }

    @Test
    fun historyIsNewestFirstAndCapped() {
        var list = emptyList<MatchResult>()
        repeat(MatchHistory.MAX_ENTRIES + 5) { list = MatchHistory.add(list, result(MatchDifficulty.EASY, 6 + it, 10, it.toLong())) }
        assertEquals(MatchHistory.MAX_ENTRIES, list.size)
        assertEquals(MatchHistory.MAX_ENTRIES + 4L, list.first().completedAtMillis)
    }

    @Test
    fun timeIsShownAsMinutesAndSeconds() {
        assertEquals("00:00", MatchHistory.formatTime(0))
        assertEquals("01:05", MatchHistory.formatTime(65))
        assertEquals("12:00", MatchHistory.formatTime(720))
    }

    @Test
    fun storageRoundTripsAndSkipsDamagedLines() {
        val list = listOf(result(MatchDifficulty.MEDIUM, 12, 33, 100), result(MatchDifficulty.HARD, 20, 80, 50))
        assertEquals(list, MatchHistoryCodec.decode(MatchHistoryCodec.encode(list)))
        val text = "junk\n" + MatchHistoryCodec.encode(list) + "\n1\teasy\t2\t5\n1\teasy\t9\t0\n1\tnope\t9\t5"
        assertEquals(list, MatchHistoryCodec.decode(text)) // fewer moves than pairs, zero seconds, unknown level
        assertEquals(emptyList<MatchResult>(), MatchHistoryCodec.decode(""))
    }
}
