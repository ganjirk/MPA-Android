package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Stores [TicTacToeStats] as plain text, one record per line, fields separated by tabs:
 * - `C games wins losses draws`
 * - `P normalizedName(url-encoded) displayName(url-encoded) games wins losses draws`
 * - `M playedAtMillis playerX(url-encoded) playerO(url-encoded) X|O|D`
 * - `G playedAtMillis W|L|D` (a finished game against the computer)
 * Lines that cannot be read are skipped instead of failing the whole file.
 */
object TicTacToeCodec {
    private const val TAB = "\t"
    private const val CHARSET = "UTF-8"

    private fun enc(text: String) = URLEncoder.encode(text, CHARSET)
    private fun dec(text: String) = runCatching { URLDecoder.decode(text, CHARSET) }.getOrNull()

    fun encode(stats: TicTacToeStats): String {
        val c = stats.computer
        val lines = mutableListOf(listOf("C", c.games, c.wins, c.losses, c.draws).joinToString(TAB))
        stats.players.forEach { p ->
            val r = p.record
            lines += listOf("P", enc(p.normalizedName), enc(p.displayName), r.games, r.wins, r.losses, r.draws).joinToString(TAB)
        }
        stats.recentMatches.forEach { m ->
            val code = when (m.winner) {
                MatchWinner.X -> "X"
                MatchWinner.O -> "O"
                MatchWinner.DRAW -> "D"
            }
            lines += listOf("M", m.playedAtMillis, enc(m.playerX), enc(m.playerO), code).joinToString(TAB)
        }
        stats.recentComputerGames.forEach { g ->
            val code = when (g.result) {
                GameResult.WIN -> "W"
                GameResult.LOSS -> "L"
                GameResult.DRAW -> "D"
            }
            lines += listOf("G", g.playedAtMillis, code).joinToString(TAB)
        }
        return lines.joinToString("\n")
    }

    fun decode(text: String): TicTacToeStats {
        var computer = GameRecord()
        val players = mutableListOf<LocalPlayer>()
        val matches = mutableListOf<LocalMatchEntry>()
        val games = mutableListOf<ComputerGameEntry>()
        for (line in text.lineSequence()) {
            val parts = line.split(TAB)
            when (parts.firstOrNull()) {
                "C" -> parseRecord(parts.drop(1))?.let { computer = it }
                "P" -> parsePlayer(parts)?.let(players::add)
                "M" -> parseMatch(parts)?.let(matches::add)
                "G" -> parseComputerGame(parts)?.let(games::add)
            }
        }
        return TicTacToeStats(computer, players, matches, games)
    }

    private fun parseRecord(numbers: List<String>): GameRecord? {
        if (numbers.size != 4) return null
        val v = numbers.map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
        return GameRecord(v[0], v[1], v[2], v[3])
    }

    private fun parsePlayer(parts: List<String>): LocalPlayer? {
        if (parts.size != 7) return null
        val normalized = dec(parts[1])?.takeIf { it.isNotBlank() } ?: return null
        val display = dec(parts[2])?.takeIf { it.isNotBlank() } ?: return null
        val record = parseRecord(parts.drop(3)) ?: return null
        return LocalPlayer(normalized, display, record)
    }

    private fun parseComputerGame(parts: List<String>): ComputerGameEntry? {
        if (parts.size != 3) return null
        val at = parts[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
        val result = when (parts[2]) {
            "W" -> GameResult.WIN
            "L" -> GameResult.LOSS
            "D" -> GameResult.DRAW
            else -> return null
        }
        return ComputerGameEntry(at, result)
    }

    private fun parseMatch(parts: List<String>): LocalMatchEntry? {
        if (parts.size != 5) return null
        val at = parts[1].toLongOrNull() ?: return null
        val x = dec(parts[2])?.takeIf { it.isNotBlank() } ?: return null
        val o = dec(parts[3])?.takeIf { it.isNotBlank() } ?: return null
        val winner = when (parts[4]) {
            "X" -> MatchWinner.X
            "O" -> MatchWinner.O
            "D" -> MatchWinner.DRAW
            else -> return null
        }
        return LocalMatchEntry(at, x, o, winner)
    }
}
