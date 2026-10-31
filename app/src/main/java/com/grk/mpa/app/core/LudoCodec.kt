package com.grk.mpa.app.core

/**
 * Stores a game in progress as plain text:
 * - `LUDO1`
 * - `T turn dice sixes phase winner moves`
 * - `S color kind p0 p1 p2 p3` once per seat (kind HUMAN|CPU|EASY, positions -1..56)
 * Anything that does not describe a consistent game is rejected and decodes to null.
 */
object LudoCodec {
    private const val HEADER = "LUDO1"

    fun encode(state: LudoState): String {
        val lines = mutableListOf(HEADER)
        lines += "T ${state.turn} ${state.dice} ${state.sixes} ${state.phase.name} ${state.winner} ${state.moves}"
        state.seats.forEachIndexed { i, seat ->
            lines += "S ${seat.color} ${seat.kind.name} ${state.pos[i].joinToString(" ")}"
        }
        return lines.joinToString("\n")
    }

    fun decode(text: String): LudoState? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.firstOrNull() != HEADER) return null
        val t = lines.getOrNull(1)?.split(" ")?.takeIf { it.size == 7 && it[0] == "T" } ?: return null
        val turn = t[1].toIntOrNull() ?: return null
        val dice = t[2].toIntOrNull() ?: return null
        val sixes = t[3].toIntOrNull() ?: return null
        val phase = LudoPhase.entries.firstOrNull { it.name == t[4] } ?: return null
        val winner = t[5].toIntOrNull() ?: return null
        val moves = t[6].toIntOrNull()?.takeIf { it >= 0 } ?: return null

        val seats = mutableListOf<LudoSeat>()
        val pos = mutableListOf<List<Int>>()
        for (line in lines.drop(2)) {
            val p = line.split(" ")
            if (p.size != 3 + Ludo.TOKENS || p[0] != "S") return null
            val color = p[1].toIntOrNull()?.takeIf { it in 0..3 } ?: return null
            val kind = LudoKind.entries.firstOrNull { it.name == p[2] } ?: return null
            val tokens = p.drop(3).map { it.toIntOrNull()?.takeIf { r -> r in -1..Ludo.HOME_REL } ?: return null }
            seats += LudoSeat(color, kind)
            pos += tokens
        }
        if (seats.size !in 2..4) return null
        if (seats.map { it.color } != Ludo.coloursFor(seats.size)) return null
        if (turn !in seats.indices || dice !in 0..6 || sixes !in 0..2) return null
        if (phase == LudoPhase.OVER) {
            if (winner !in seats.indices || !pos[winner].all { it == Ludo.HOME_REL }) return null
        } else if (winner != -1) return null
        if (phase == LudoPhase.MOVE && dice == 0) return null
        return LudoState(seats, pos, turn, dice, sixes, phase, winner, emptyList(), moves)
    }
}
