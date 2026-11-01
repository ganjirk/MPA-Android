package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeCodecTest {
    @Test
    fun emptyStatsRoundTrip() {
        assertEquals(TicTacToeStats(), TicTacToeCodec.decode(TicTacToeCodec.encode(TicTacToeStats())))
        assertEquals(TicTacToeStats(), TicTacToeCodec.decode(""))
    }

    @Test
    fun fullStatsRoundTripWithAwkwardNames() {
        val stats = TicTacToeStats(
            computer = GameRecord(10, 4, 5, 1),
            players = listOf(
                LocalPlayer("asha", "Asha", GameRecord(3, 2, 1, 0)),
                LocalPlayer("tab\tnew\nline", "Tab\tNew\nLine", GameRecord(1, 0, 0, 1)),
                LocalPlayer("日本語 & 100%", "日本語 & 100%", GameRecord(2, 1, 0, 1))
            ),
            recentMatches = listOf(
                LocalMatchEntry(200, "Asha", "Tab\tNew\nLine", MatchWinner.X),
                LocalMatchEntry(100, "日本語 & 100%", "Asha", MatchWinner.DRAW),
                LocalMatchEntry(50, "Asha", "日本語 & 100%", MatchWinner.O)
            )
        )
        val encoded = TicTacToeCodec.encode(stats)
        assertEquals("one line per record", 1 + 3 + 3, encoded.lines().size)
        assertEquals(stats, TicTacToeCodec.decode(encoded))
    }

    @Test
    fun malformedLinesAreSkipped() {
        val text = listOf(
            "C\t1\t1\t0\t0",
            "C\tnope",
            "P\tasha\tAsha\t1\t1\t0",
            "P\t%ZZ\tAsha\t1\t1\t0\t0",
            "P\tasha\tAsha\t-1\t1\t0\t0",
            "M\tnot-a-number\tA\tB\tX",
            "M\t1\tA\tB\tQ",
            "garbage",
            "M\t7\tA\tB\tD"
        ).joinToString("\n")
        val stats = TicTacToeCodec.decode(text)
        assertEquals(GameRecord(1, 1, 0, 0), stats.computer)
        assertTrue(stats.players.isEmpty())
        assertEquals(listOf(LocalMatchEntry(7, "A", "B", MatchWinner.DRAW)), stats.recentMatches)
    }

    @Test
    fun computerGameTimesRoundTrip() {
        val stats = TicTacToeStats(
            recentComputerGames = listOf(
                ComputerGameEntry(300, GameResult.WIN),
                ComputerGameEntry(200, GameResult.LOSS),
                ComputerGameEntry(100, GameResult.DRAW)
            )
        )
        assertEquals(stats, TicTacToeCodec.decode(TicTacToeCodec.encode(stats)))
    }
}
