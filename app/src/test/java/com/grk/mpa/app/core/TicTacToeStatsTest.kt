package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeStatsTest {
    private val ops = TicTacToeStatsOperations

    @Test
    fun computerGamesAreCountedByResult() {
        var stats = TicTacToeStats()
        stats = ops.recordComputerGame(stats, GameResult.WIN)
        stats = ops.recordComputerGame(stats, GameResult.LOSS)
        stats = ops.recordComputerGame(stats, GameResult.LOSS)
        stats = ops.recordComputerGame(stats, GameResult.DRAW)
        assertEquals(GameRecord(games = 4, wins = 1, losses = 2, draws = 1), stats.computer)
    }

    @Test
    fun namesMustBeOneToTwentyFourCharactersAndDifferent() {
        assertNull(ops.validateNames("Asha", "Ravi"))
        assertEquals(NameError.LENGTH, ops.validateNames("", "Ravi"))
        assertEquals(NameError.LENGTH, ops.validateNames("Asha", "   "))
        assertEquals(NameError.LENGTH, ops.validateNames("x".repeat(25), "Ravi"))
        assertNull(ops.validateNames("x".repeat(24), "Ravi"))
        assertEquals(NameError.LENGTH, ops.validateNames("Bad\u0007name", "Ravi"))
    }

    @Test
    fun namesAreComparedIgnoringCaseAndSpaces() {
        assertEquals(NameError.SAME, ops.validateNames("Asha", " aSHA "))
    }

    @Test
    fun localMatchUpdatesBothPlayersAndTheRecentList() {
        val stats = ops.recordLocalMatch(TicTacToeStats(), "Asha", "Ravi", MatchWinner.X, nowMillis = 100)
        val byName = stats.players.associateBy { it.normalizedName }
        assertEquals(GameRecord(1, 1, 0, 0), byName.getValue("asha").record)
        assertEquals(GameRecord(1, 0, 1, 0), byName.getValue("ravi").record)
        assertEquals(listOf(LocalMatchEntry(100, "Asha", "Ravi", MatchWinner.X)), stats.recentMatches)
    }

    @Test
    fun drawCountsForBothPlayers() {
        val stats = ops.recordLocalMatch(TicTacToeStats(), "Asha", "Ravi", MatchWinner.DRAW, 1)
        assertTrue(stats.players.all { it.record == GameRecord(1, 0, 0, 1) })
    }

    @Test
    fun returningPlayersKeepTheirFirstDisplayNameAndAccumulate() {
        var stats = ops.recordLocalMatch(TicTacToeStats(), "Asha", "Ravi", MatchWinner.X, 1)
        stats = ops.recordLocalMatch(stats, "RAVI", "asha", MatchWinner.X, 2)
        assertEquals(2, stats.players.size)
        val asha = stats.players.first { it.normalizedName == "asha" }
        assertEquals("Asha", asha.displayName)
        assertEquals(GameRecord(2, 1, 1, 0), asha.record)
    }

    @Test
    fun invalidNamesLeaveTheStatsUntouched() {
        val stats = TicTacToeStats()
        assertSame(stats, ops.recordLocalMatch(stats, "Asha", "asha", MatchWinner.X, 1))
        assertSame(stats, ops.recordLocalMatch(stats, "", "Ravi", MatchWinner.X, 1))
    }

    @Test
    fun recentMatchesAreNewestFirstAndCapped() {
        var stats = TicTacToeStats()
        repeat(ops.MAX_RECENT_MATCHES + 5) { i ->
            stats = ops.recordLocalMatch(stats, "A", "B", MatchWinner.DRAW, nowMillis = i.toLong())
        }
        assertEquals(ops.MAX_RECENT_MATCHES, stats.recentMatches.size)
        assertEquals((ops.MAX_RECENT_MATCHES + 4).toLong(), stats.recentMatches.first().playedAtMillis)
    }

    @Test
    fun leaderboardOrdersByWinsThenGamesThenDrawsThenName() {
        fun player(name: String, g: Int, w: Int, l: Int, d: Int) =
            LocalPlayer(name.lowercase(), name, GameRecord(g, w, l, d))
        val ordered = ops.leaderboard(
            listOf(
                player("zed", 4, 2, 2, 0),
                player("amy", 4, 2, 2, 0),
                player("bob", 5, 2, 3, 0),
                player("cat", 3, 3, 0, 0),
                player("dan", 4, 2, 1, 1),
                player("idle", 0, 0, 0, 0)
            )
        )
        assertEquals(listOf("cat", "bob", "dan", "amy", "zed"), ordered.map { it.displayName })
    }

    @Test
    fun computerGamesKeepTheirTimeOnlyWhenOneIsGiven() {
        val untimed = ops.recordComputerGame(TicTacToeStats(), GameResult.WIN)
        assertTrue(untimed.recentComputerGames.isEmpty())
        val timed = ops.recordComputerGame(untimed, GameResult.LOSS, 500)
        assertEquals(listOf(ComputerGameEntry(500, GameResult.LOSS)), timed.recentComputerGames)
        assertEquals(2, timed.computer.games)
    }
}
