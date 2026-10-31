package com.grk.mpa.app.core

import kotlin.random.Random

/** Who controls a seat. [EASY] is a computer that plays a random legal move half of the time. */
enum class LudoKind { HUMAN, CPU, EASY }

enum class LudoPhase { ROLL, MOVE, OVER }

/** A square on the 15 x 15 board, counted from the top left. */
data class LudoCell(val row: Int, val col: Int)

/** [color]: 0 red (top left), 1 green (top right), 2 yellow (bottom right), 3 blue (bottom left). */
data class LudoSeat(val color: Int, val kind: LudoKind)

sealed interface LudoEvent {
    val seat: Int

    data class Rolled(override val seat: Int, val value: Int) : LudoEvent
    data class NoMove(override val seat: Int) : LudoEvent
    data class TripleSix(override val seat: Int) : LudoEvent
    data class Moved(override val seat: Int, val token: Int, val from: Int, val to: Int, val value: Int) : LudoEvent
    data class ReachedHome(override val seat: Int, val token: Int) : LudoEvent
    data class Captured(override val seat: Int, val victimSeat: Int, val token: Int) : LudoEvent
    data class Won(override val seat: Int) : LudoEvent
}

/**
 * A token's position (`rel`): -1 in the base, 0 on its own start square, 1..50 further along the shared track,
 * 51..55 on its private home column and 56 home.
 */
data class LudoState(
    val seats: List<LudoSeat>,
    val pos: List<List<Int>>,
    val turn: Int = 0,
    val dice: Int = 0,
    val sixes: Int = 0,
    val phase: LudoPhase = LudoPhase.ROLL,
    val winner: Int = -1,
    val events: List<LudoEvent> = emptyList(),
    val moves: Int = 0
) {
    val isOver: Boolean get() = phase == LudoPhase.OVER
}

/**
 * Ludo rules and simple computer players, ported from the website (`Ludo/static/Ludo/ludo-engine.js`).
 * Every function returns a new state; nothing is changed in place.
 */
object Ludo {
    const val HOME_REL = 56
    const val LAST_TRACK_REL = 50
    const val TRACK_SIZE = 52
    const val BOARD_SIZE = 15
    const val TOKENS = 4

    val START = listOf(0, 13, 26, 39)
    val STARS = listOf(8, 21, 34, 47)
    private val SAFE: Set<Int> = (START + STARS).toSet()

    /** The shared track, clockwise from red's start square. */
    val TRACK: List<LudoCell> = buildList {
        fun run(r: Int, c: Int, dr: Int, dc: Int, n: Int) {
            for (i in 0 until n) add(LudoCell(r + dr * i, c + dc * i))
        }
        run(6, 1, 0, 1, 5)
        run(5, 6, -1, 0, 6)
        run(0, 7, 0, 1, 2)
        run(1, 8, 1, 0, 5)
        run(6, 9, 0, 1, 6)
        run(7, 14, 1, 0, 2)
        run(8, 13, 0, -1, 5)
        run(9, 8, 1, 0, 6)
        run(14, 7, 0, -1, 2)
        run(13, 6, -1, 0, 5)
        run(8, 5, 0, -1, 6)
        run(7, 0, -1, 0, 2)
    }

    /** Private home columns, five squares each, ending next to the centre. */
    val HOME: List<List<LudoCell>> = listOf(
        (1..5).map { LudoCell(7, it) },
        (1..5).map { LudoCell(it, 7) },
        (13 downTo 9).map { LudoCell(7, it) },
        (13 downTo 9).map { LudoCell(it, 7) }
    )

    /** Where a finished token rests, inside the centre block. */
    val FINISH: List<LudoCell> = listOf(LudoCell(7, 6), LudoCell(6, 7), LudoCell(7, 8), LudoCell(8, 7))

    /** Four resting places in each base. */
    val BASE: List<List<LudoCell>> = listOf(
        listOf(LudoCell(2, 2), LudoCell(2, 3), LudoCell(3, 2), LudoCell(3, 3)),
        listOf(LudoCell(2, 11), LudoCell(2, 12), LudoCell(3, 11), LudoCell(3, 12)),
        listOf(LudoCell(11, 11), LudoCell(11, 12), LudoCell(12, 11), LudoCell(12, 12)),
        listOf(LudoCell(11, 2), LudoCell(11, 3), LudoCell(12, 2), LudoCell(12, 3))
    )

    /** Index on the shared track, or -1 when the token is in its base, on its home column or home. */
    fun trackIndex(color: Int, rel: Int): Int =
        if (rel in 0..LAST_TRACK_REL) (START[color] + rel) % TRACK_SIZE else -1

    fun isSafe(index: Int): Boolean = index in SAFE

    fun cellOf(color: Int, rel: Int, slot: Int): LudoCell = when {
        rel < 0 -> BASE[color][slot]
        rel <= LAST_TRACK_REL -> TRACK[(START[color] + rel) % TRACK_SIZE]
        rel < HOME_REL -> HOME[color][rel - (LAST_TRACK_REL + 1)]
        else -> FINISH[color]
    }

    /** Colours in play for 2, 3 or 4 players. Two players sit opposite each other. */
    fun coloursFor(players: Int): List<Int> = when {
        players <= 2 -> listOf(0, 2)
        players == 3 -> listOf(0, 1, 2)
        else -> listOf(0, 1, 2, 3)
    }

    /** [kinds] has one entry per player (2 to 4). */
    fun newGame(kinds: List<LudoKind>): LudoState {
        require(kinds.size in 2..4) { "Ludo needs 2 to 4 players" }
        val colours = coloursFor(kinds.size)
        return LudoState(
            seats = kinds.mapIndexed { i, k -> LudoSeat(colours[i], k) },
            pos = kinds.map { List(TOKENS) { -1 } }
        )
    }

    /** Tokens of the player whose turn it is that can move by the rolled number. */
    fun legalTokens(state: LudoState): List<Int> {
        if (state.phase != LudoPhase.MOVE) return emptyList()
        return state.pos[state.turn].indices.filter { i ->
            val rel = state.pos[state.turn][i]
            if (rel == -1) state.dice == 6 else rel < HOME_REL && rel + state.dice <= HOME_REL
        }
    }

    private fun nextTurn(s: LudoState): LudoState =
        s.copy(turn = (s.turn + 1) % s.seats.size, sixes = 0, phase = LudoPhase.ROLL)

    /** Roll with a given value (1 to 6). */
    fun rollDice(state: LudoState, value: Int): LudoState {
        require(value in 1..6) { "A dice shows 1 to 6" }
        if (state.phase != LudoPhase.ROLL) return state
        val events = mutableListOf<LudoEvent>(LudoEvent.Rolled(state.turn, value))
        val sixes = if (value == 6) state.sixes + 1 else 0
        var s = state.copy(dice = value, sixes = sixes, events = events)
        if (sixes >= 3) {
            events += LudoEvent.TripleSix(s.turn)
            return nextTurn(s).copy(events = events)
        }
        s = s.copy(phase = LudoPhase.MOVE)
        if (legalTokens(s).isEmpty()) {
            events += LudoEvent.NoMove(s.turn)
            s = nextTurn(s).copy(events = events)
        }
        return s
    }

    /** Move one of the current player's tokens by the rolled number. Illegal moves return [state] unchanged. */
    fun moveToken(state: LudoState, token: Int): LudoState {
        if (token !in legalTokens(state)) return state
        val seat = state.turn
        val color = state.seats[seat].color
        val pos = state.pos.map { it.toMutableList() }
        val from = pos[seat][token]
        val to = if (from == -1) 0 else from + state.dice
        pos[seat][token] = to
        val events = mutableListOf<LudoEvent>(LudoEvent.Moved(seat, token, from, to, state.dice))
        if (to == HOME_REL) events += LudoEvent.ReachedHome(seat, token)

        val idx = trackIndex(color, to)
        var captured = false
        if (idx >= 0 && !isSafe(idx)) {
            for (o in state.seats.indices) {
                if (o == seat) continue
                for (j in 0 until TOKENS) {
                    if (trackIndex(state.seats[o].color, pos[o][j]) == idx) {
                        pos[o][j] = -1
                        events += LudoEvent.Captured(seat, o, j)
                        captured = true
                    }
                }
            }
        }
        val moved = state.copy(pos = pos.map { it.toList() }, events = events, moves = state.moves + 1)
        if (pos[seat].all { it == HOME_REL }) {
            return moved.copy(phase = LudoPhase.OVER, winner = seat, events = events + LudoEvent.Won(seat))
        }
        return if (state.dice == 6 || captured) moved.copy(phase = LudoPhase.ROLL) else nextTurn(moved)
    }

    // ---- Computer players ----

    /** Could an opponent 1 to 6 squares behind [idx] on the shared track hit it next turn? */
    private fun threatened(s: LudoState, seat: Int, idx: Int): Boolean {
        for (o in s.seats.indices) {
            if (o == seat) continue
            for (j in 0 until TOKENS) {
                val oi = trackIndex(s.seats[o].color, s.pos[o][j])
                if (oi < 0) continue
                val gap = (idx - oi + TRACK_SIZE) % TRACK_SIZE
                if (gap in 1..6) return true
            }
        }
        return false
    }

    internal fun scoreMove(s: LudoState, token: Int): Double {
        val seat = s.turn
        val color = s.seats[seat].color
        val from = s.pos[seat][token]
        val to = if (from == -1) 0 else from + s.dice
        var score = 0.0
        if (to == HOME_REL) score += 1000 else if (to > LAST_TRACK_REL) score += 300
        val idx = trackIndex(color, to)
        val fromIdx = trackIndex(color, from)
        if (idx >= 0 && !isSafe(idx)) {
            for (o in s.seats.indices) {
                if (o == seat) continue
                for (j in 0 until TOKENS) {
                    if (trackIndex(s.seats[o].color, s.pos[o][j]) == idx) score += 500 + s.pos[o][j] * 3
                }
            }
            if (threatened(s, seat, idx)) score -= 120
        }
        if (idx >= 0 && isSafe(idx)) score += 120
        if (from == -1) score += 400
        if (fromIdx >= 0 && !isSafe(fromIdx) && threatened(s, seat, fromIdx)) score += 150
        return score + to / 2.0
    }

    /** The token the computer would move, or -1 when none can. */
    fun chooseToken(s: LudoState, kind: LudoKind, random: Random = Random.Default): Int {
        val legal = legalTokens(s)
        if (legal.isEmpty()) return -1
        if (legal.size == 1) return legal[0]
        if (kind == LudoKind.EASY && random.nextDouble() < 0.5) return legal[random.nextInt(legal.size)]
        var best = Double.NEGATIVE_INFINITY
        val pool = mutableListOf<Int>()
        for (t in legal) {
            val sc = scoreMove(s, t)
            if (sc > best + 1e-9) {
                best = sc
                pool.clear()
                pool += t
            } else if (kotlin.math.abs(sc - best) <= 1e-9) pool += t
        }
        return pool[random.nextInt(pool.size)]
    }

    fun rollValue(random: Random = Random.Default): Int = random.nextInt(1, 7)
}

/** Where a token is drawn, in board cells; tokens sharing a square are made smaller and spread out. */
data class LudoTokenPlacement(val seat: Int, val token: Int, val x: Float, val y: Float, val size: Float)

object LudoBoardLayout {
    private const val TOKEN = 0.78f
    private val STACK = mapOf(
        2 to listOf(-0.2f to -0.2f, 0.2f to 0.2f),
        3 to listOf(-0.22f to -0.22f, 0.22f to -0.22f, 0f to 0.24f),
        4 to listOf(-0.22f to -0.22f, 0.22f to -0.22f, -0.22f to 0.22f, 0.22f to 0.22f)
    )

    /** Centre (x = column, y = row) and diameter of every token, in cell units. */
    fun placements(state: LudoState): List<LudoTokenPlacement> {
        data class Entry(val seat: Int, val token: Int, val cell: LudoCell, val key: String)

        val entries = state.seats.indices.flatMap { si ->
            val color = state.seats[si].color
            state.pos[si].mapIndexed { i, rel ->
                val cell = Ludo.cellOf(color, rel, i)
                Entry(si, i, cell, if (rel < 0) "b$color$i" else "${cell.row},${cell.col}")
            }
        }
        val groups = entries.groupBy { it.key }
        return entries.map { e ->
            val group = groups.getValue(e.key)
            val n = minOf(group.size, 4)
            val at = group.indexOfFirst { it.seat == e.seat && it.token == e.token }
            var size = TOKEN
            var ox = 0f
            var oy = 0f
            if (n > 1) {
                size = TOKEN * 0.62f
                val o = STACK.getValue(n)[minOf(at, 3)]
                ox = o.first * TOKEN * 0.5f
                oy = o.second * TOKEN * 0.5f
            }
            LudoTokenPlacement(e.seat, e.token, e.cell.col + 0.5f + ox, e.cell.row + 0.5f + oy, size)
        }
    }
}
