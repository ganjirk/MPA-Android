package com.grk.mpa.app.core

enum class ChessMode { COMPUTER, TWO_PLAYERS }

/** A move as stored: only where it starts, where it ends and what a pawn becomes. */
data class ChessMoveRef(val from: Int, val to: Int, val promo: Int = 0)

/** One move played, with what is needed to show it and to go back. [captured] is a signed piece, 0 for none. */
data class ChessEntry(val move: ChessMove, val san: String, val before: ChessPosition, val captured: Int)

/** [side] is the human's colour against the computer: 1 white, -1 black. [resignedLoser] is 0 while nobody resigned. */
data class ChessSave(
    val mode: ChessMode,
    val side: Int,
    val level: ChessLevel,
    val flipped: Boolean,
    val resignedLoser: Int,
    val moves: List<ChessMoveRef>
)

object ChessGame {
    fun entry(before: ChessPosition, move: ChessMove, legal: List<ChessMove>): ChessEntry {
        val captured = if (move.flag == ChessFlag.EP) -before.turn * Chess.P else before.board[move.to]
        return ChessEntry(move, Chess.san(before, move, legal), before, captured)
    }

    /** Plays the stored moves from the start. Returns null if any of them is not legal. */
    fun replay(moves: List<ChessMoveRef>): Pair<ChessPosition, List<ChessEntry>>? {
        var position = Chess.newGame()
        val entries = mutableListOf<ChessEntry>()
        for (ref in moves) {
            val legal = Chess.legalMoves(position)
            val move = legal.firstOrNull { it.from == ref.from && it.to == ref.to && it.promo == ref.promo } ?: return null
            entries += entry(position, move, legal)
            position = Chess.makeMove(position, move)
        }
        return position to entries
    }
}

/**
 * Plain text storage:
 * - `CHESS1`
 * - `S mode side level flipped resignedLoser` (mode COMPUTER|TWO_PLAYERS, side 1|-1, level EASY|MEDIUM|HARD, flipped 0|1)
 * - `M from-to-promo from-to-promo ...`
 */
object ChessCodec {
    private const val HEADER = "CHESS1"

    fun encode(save: ChessSave): String = listOf(
        HEADER,
        "S ${save.mode.name} ${save.side} ${save.level.name} ${if (save.flipped) 1 else 0} ${save.resignedLoser}",
        "M " + save.moves.joinToString(" ") { "${it.from}-${it.to}-${it.promo}" }
    ).joinToString("\n")

    fun decode(text: String): ChessSave? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.firstOrNull() != HEADER) return null
        val s = lines.getOrNull(1)?.split(" ")?.takeIf { it.size == 6 && it[0] == "S" } ?: return null
        val mode = ChessMode.entries.firstOrNull { it.name == s[1] } ?: return null
        val side = s[2].toIntOrNull()?.takeIf { it == 1 || it == -1 } ?: return null
        val level = ChessLevel.entries.firstOrNull { it.name == s[3] } ?: return null
        val flipped = when (s[4]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        val loser = s[5].toIntOrNull()?.takeIf { it in -1..1 } ?: return null
        val moveLine = lines.getOrNull(2)?.takeIf { it == "M" || it.startsWith("M ") } ?: return null
        val moves = moveLine.drop(1).trim().split(" ").filter { it.isNotEmpty() }.map { token ->
            val p = token.split("-")
            if (p.size != 3) return null
            val from = p[0].toIntOrNull()?.takeIf { it in 0..63 } ?: return null
            val to = p[1].toIntOrNull()?.takeIf { it in 0..63 } ?: return null
            val promo = p[2].toIntOrNull()?.takeIf { it in 0..5 } ?: return null
            ChessMoveRef(from, to, promo)
        }
        return ChessSave(mode, side, level, flipped, loser, moves)
    }
}
