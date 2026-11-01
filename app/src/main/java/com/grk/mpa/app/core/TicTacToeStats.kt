package com.grk.mpa.app.core

import java.util.Locale

/** Games played and their results. */
data class GameRecord(val games: Int = 0, val wins: Int = 0, val losses: Int = 0, val draws: Int = 0) {
    fun withResult(result: GameResult): GameRecord = copy(
        games = games + 1,
        wins = wins + if (result == GameResult.WIN) 1 else 0,
        losses = losses + if (result == GameResult.LOSS) 1 else 0,
        draws = draws + if (result == GameResult.DRAW) 1 else 0
    )
}

enum class GameResult { WIN, LOSS, DRAW }

enum class MatchWinner { X, O, DRAW }

/** A named player in two-player mode. Names are matched ignoring case, as on the website. */
data class LocalPlayer(val normalizedName: String, val displayName: String, val record: GameRecord)

data class LocalMatchEntry(
    val playedAtMillis: Long,
    val playerX: String,
    val playerO: String,
    val winner: MatchWinner
)

/** A finished game against the computer, kept so "Your week" can count it. */
data class ComputerGameEntry(val playedAtMillis: Long, val result: GameResult)

data class TicTacToeStats(
    /** Your results against the computer. */
    val computer: GameRecord = GameRecord(),
    val players: List<LocalPlayer> = emptyList(),
    /** Newest first. */
    val recentMatches: List<LocalMatchEntry> = emptyList(),
    /** Newest first; only games finished after this list was introduced have an entry. */
    val recentComputerGames: List<ComputerGameEntry> = emptyList()
)

enum class NameError { LENGTH, SAME }

/** Pure functions on [TicTacToeStats]. */
object TicTacToeStatsOperations {
    const val MAX_NAME_LENGTH = 24
    const val MAX_RECENT_MATCHES = 50

    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    /** The website's rules: 1 to 24 characters, no control characters, and the two names must differ. */
    fun validateNames(playerX: String, playerO: String): NameError? {
        val x = playerX.trim()
        val o = playerO.trim()
        for (name in listOf(x, o)) {
            if (name.isEmpty() || name.length > MAX_NAME_LENGTH || name.any { it.code < 32 }) return NameError.LENGTH
        }
        return if (normalize(x) == normalize(o)) NameError.SAME else null
    }

    fun recordComputerGame(stats: TicTacToeStats, result: GameResult, nowMillis: Long = 0L): TicTacToeStats =
        stats.copy(
            computer = stats.computer.withResult(result),
            recentComputerGames = if (nowMillis > 0L) {
                (listOf(ComputerGameEntry(nowMillis, result)) + stats.recentComputerGames).take(MAX_RECENT_MATCHES)
            } else stats.recentComputerGames
        )

    /** Saves a finished two-player match. Returns [stats] unchanged if the names are invalid. */
    fun recordLocalMatch(
        stats: TicTacToeStats,
        playerX: String,
        playerO: String,
        winner: MatchWinner,
        nowMillis: Long
    ): TicTacToeStats {
        if (validateNames(playerX, playerO) != null) return stats
        val x = playerX.trim()
        val o = playerO.trim()
        val resultForX = when (winner) {
            MatchWinner.X -> GameResult.WIN
            MatchWinner.O -> GameResult.LOSS
            MatchWinner.DRAW -> GameResult.DRAW
        }
        val resultForO = when (resultForX) {
            GameResult.WIN -> GameResult.LOSS
            GameResult.LOSS -> GameResult.WIN
            GameResult.DRAW -> GameResult.DRAW
        }
        val players = updatePlayer(updatePlayer(stats.players, x, resultForX), o, resultForO)
        val matches = (listOf(LocalMatchEntry(nowMillis, x, o, winner)) + stats.recentMatches).take(MAX_RECENT_MATCHES)
        return stats.copy(players = players, recentMatches = matches)
    }

    private fun updatePlayer(players: List<LocalPlayer>, name: String, result: GameResult): List<LocalPlayer> {
        val key = normalize(name)
        return if (players.any { it.normalizedName == key }) {
            players.map { if (it.normalizedName == key) it.copy(record = it.record.withResult(result)) else it }
        } else {
            players + LocalPlayer(key, name, GameRecord().withResult(result))
        }
    }

    /** Most wins first, then most games, then most draws, then name. */
    fun leaderboard(players: List<LocalPlayer>): List<LocalPlayer> = players
        .filter { it.record.games > 0 }
        .sortedWith(
            compareByDescending<LocalPlayer> { it.record.wins }
                .thenByDescending { it.record.games }
                .thenByDescending { it.record.draws }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        )
}
