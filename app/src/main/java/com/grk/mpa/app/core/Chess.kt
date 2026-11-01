package com.grk.mpa.app.core

import kotlin.random.Random

/**
 * Chess rules and a small computer opponent, ported from the website (`Chess/static/Chess/chess-engine.js`).
 * Squares are numbered 0..63 from a8 (0) to h1 (63). Pieces are signed numbers: 1 pawn, 2 knight, 3 bishop,
 * 4 rook, 5 queen, 6 king; positive = white, negative = black. Colours are 1 (white) and -1 (black).
 */
enum class ChessFlag { NORMAL, DOUBLE, EP, CASTLE_K, CASTLE_Q }

data class ChessMove(val from: Int, val to: Int, val promo: Int = 0, val flag: ChessFlag = ChessFlag.NORMAL)

/** Treat as immutable: [board] is never changed after a position is built. */
class ChessPosition(
    val board: IntArray,
    val turn: Int,
    val castle: Int,
    val ep: Int,
    val half: Int,
    val full: Int,
    /** Position keys of the game so far, for the threefold repetition rule. */
    val keys: List<String> = emptyList()
)

enum class ChessResult { CHECKMATE, STALEMATE, FIFTY, REPETITION, MATERIAL, RESIGN }

/** [winner] is 1, -1, or 0 for a draw. */
data class ChessStatus(val over: Boolean, val result: ChessResult?, val winner: Int, val check: Boolean)

enum class ChessLevel(val depth: Int, val margin: Int, val timeMs: Long) {
    EASY(1, 220, 0),
    MEDIUM(2, 30, 0),
    HARD(6, 0, 1200)
}

object Chess {
    const val P = 1
    const val N = 2
    const val B = 3
    const val R = 4
    const val Q = 5
    const val K = 6

    const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private const val LETTERS = "pnbrqk"
    private const val FILES = "abcdefgh"

    private val KNIGHT = Array(64) { IntArray(0) }
    private val KING = Array(64) { IntArray(0) }

    /** Per square: eight rays, 0-3 straight (rook), 4-7 diagonal (bishop). */
    private val RAYS: Array<Array<IntArray>>

    init {
        val jumps = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        val dirs = listOf(-1 to 0, 1 to 0, 0 to 1, 0 to -1, -1 to 1, -1 to -1, 1 to 1, 1 to -1)
        val rays = Array(64) { Array(8) { IntArray(0) } }
        for (sq in 0 until 64) {
            val r = sq shr 3
            val f = sq and 7
            KNIGHT[sq] = jumps.mapNotNull { (dr, df) ->
                val nr = r + dr
                val nf = f + df
                if (nr in 0..7 && nf in 0..7) nr * 8 + nf else null
            }.toIntArray()
            val kingTargets = mutableListOf<Int>()
            dirs.forEachIndexed { d, (dr, df) ->
                val ray = mutableListOf<Int>()
                var nr = r + dr
                var nf = f + df
                while (nr in 0..7 && nf in 0..7) {
                    ray += nr * 8 + nf
                    nr += dr
                    nf += df
                }
                rays[sq][d] = ray.toIntArray()
                if (ray.isNotEmpty()) kingTargets += ray[0]
            }
            KING[sq] = kingTargets.toIntArray()
        }
        RAYS = rays
    }

    /** Castling rights that survive a move from or to each square. */
    private val CR = IntArray(64) { 15 }.also {
        it[63] = 14; it[56] = 13; it[7] = 11; it[0] = 7; it[60] = 12; it[4] = 3
    }

    fun sqName(sq: Int): String = "${FILES[sq and 7]}${8 - (sq shr 3)}"
    private fun nameToSq(n: String): Int = (8 - (n[1] - '0')) * 8 + FILES.indexOf(n[0])

    // ---- Positions ----

    fun fromFen(fen: String): ChessPosition {
        val parts = fen.trim().split(Regex("\\s+"))
        val rows = parts[0].split("/")
        val b = IntArray(64)
        for (r in 0 until 8) {
            var f = 0
            for (ch in rows[r]) {
                if (ch in '1'..'8') {
                    f += ch - '0'
                    continue
                }
                val low = ch.lowercaseChar()
                b[r * 8 + f] = (if (ch == low) -1 else 1) * (LETTERS.indexOf(low) + 1)
                f++
            }
        }
        val c = parts.getOrElse(2) { "-" }
        val castle = (if ('K' in c) 1 else 0) or (if ('Q' in c) 2 else 0) or
            (if ('k' in c) 4 else 0) or (if ('q' in c) 8 else 0)
        val ep = parts.getOrNull(3)?.takeIf { it != "-" }?.let(::nameToSq) ?: -1
        val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val full = parts.getOrNull(5)?.toIntOrNull() ?: 1
        val turn = if (parts.getOrNull(1) == "b") -1 else 1
        val s = ChessPosition(b, turn, castle, ep, half, full)
        return ChessPosition(b, turn, castle, ep, half, full, listOf(positionKey(s)))
    }

    fun toFen(s: ChessPosition): String {
        val rows = (0 until 8).joinToString("/") { r ->
            val sb = StringBuilder()
            var empty = 0
            for (f in 0 until 8) {
                val p = s.board[r * 8 + f]
                if (p == 0) {
                    empty++
                    continue
                }
                if (empty > 0) sb.append(empty)
                empty = 0
                val ch = LETTERS[Math.abs(p) - 1]
                sb.append(if (p > 0) ch.uppercaseChar() else ch)
            }
            if (empty > 0) sb.append(empty)
            sb.toString()
        }
        val cs = (if (s.castle and 1 != 0) "K" else "") + (if (s.castle and 2 != 0) "Q" else "") +
            (if (s.castle and 4 != 0) "k" else "") + (if (s.castle and 8 != 0) "q" else "")
        return "$rows ${if (s.turn == 1) "w" else "b"} ${cs.ifEmpty { "-" }} " +
            "${if (s.ep >= 0) sqName(s.ep) else "-"} ${s.half} ${s.full}"
    }

    /** Same key = same position for the repetition rule (an en passant square only counts if it can be used). */
    private fun positionKey(s: ChessPosition): String {
        var ep = -1
        if (s.ep >= 0) {
            val c = s.turn
            val row = if (c == 1) 3 else 4
            val f = s.ep and 7
            if ((f > 0 && s.board[row * 8 + f - 1] == c * P) || (f < 7 && s.board[row * 8 + f + 1] == c * P)) ep = s.ep
        }
        return s.board.joinToString(",") + "|" + s.turn + "|" + s.castle + "|" + ep
    }

    fun newGame(): ChessPosition = fromFen(START_FEN)

    // ---- Attacks and move generation ----

    private fun findKing(b: IntArray, c: Int): Int {
        val k = c * K
        for (i in 0 until 64) if (b[i] == k) return i
        return -1
    }

    /** Is [sq] attacked by any piece of colour [by]? */
    private fun attacked(b: IntArray, sq: Int, by: Int): Boolean {
        val r = sq shr 3
        val f = sq and 7
        val pr = r + if (by == 1) 1 else -1
        if (pr in 0..7) {
            if (f > 0 && b[pr * 8 + f - 1] == by * P) return true
            if (f < 7 && b[pr * 8 + f + 1] == by * P) return true
        }
        for (t in KNIGHT[sq]) if (b[t] == by * N) return true
        for (t in KING[sq]) if (b[t] == by * K) return true
        for (d in 0 until 8) {
            val slider = if (d < 4) R else B
            for (t in RAYS[sq][d]) {
                val p = b[t]
                if (p == 0) continue
                if (p == by * Q || p == by * slider) return true
                break
            }
        }
        return false
    }

    private fun genMoves(s: ChessPosition, capturesOnly: Boolean): MutableList<ChessMove> {
        val b = s.board
        val c = s.turn
        val out = ArrayList<ChessMove>(48)
        fun addPromos(from: Int, to: Int) {
            out += ChessMove(from, to, Q); out += ChessMove(from, to, R)
            out += ChessMove(from, to, B); out += ChessMove(from, to, N)
        }
        for (sq in 0 until 64) {
            val p = b[sq]
            if (p * c <= 0) continue
            val t = p * c
            if (t == P) {
                val r = sq shr 3
                val f = sq and 7
                val dr = if (c == 1) -1 else 1
                val promoRow = if (c == 1) 0 else 7
                val startRow = if (c == 1) 6 else 1
                val one = sq + dr * 8
                if (b[one] == 0) {
                    if ((one shr 3) == promoRow) addPromos(sq, one)
                    else if (!capturesOnly) {
                        out += ChessMove(sq, one)
                        val two = sq + dr * 16
                        if (r == startRow && b[two] == 0) out += ChessMove(sq, two, 0, ChessFlag.DOUBLE)
                    }
                }
                for (df in intArrayOf(-1, 1)) {
                    val nf = f + df
                    if (nf < 0 || nf > 7) continue
                    val to = (r + dr) * 8 + nf
                    val q = b[to]
                    if (q * c < 0) {
                        if ((to shr 3) == promoRow) addPromos(sq, to) else out += ChessMove(sq, to)
                    } else if (q == 0 && to == s.ep) out += ChessMove(sq, to, 0, ChessFlag.EP)
                }
            } else if (t == N || t == K) {
                val targets = if (t == N) KNIGHT[sq] else KING[sq]
                for (to in targets) {
                    val q = b[to]
                    if (q * c > 0 || (capturesOnly && q == 0)) continue
                    out += ChessMove(sq, to)
                }
                if (t == K && !capturesOnly) {
                    if (c == 1 && sq == 60) {
                        if ((s.castle and 1) != 0 && b[61] == 0 && b[62] == 0 && b[63] == R &&
                            !attacked(b, 60, -1) && !attacked(b, 61, -1) && !attacked(b, 62, -1)
                        ) out += ChessMove(60, 62, 0, ChessFlag.CASTLE_K)
                        if ((s.castle and 2) != 0 && b[59] == 0 && b[58] == 0 && b[57] == 0 && b[56] == R &&
                            !attacked(b, 60, -1) && !attacked(b, 59, -1) && !attacked(b, 58, -1)
                        ) out += ChessMove(60, 58, 0, ChessFlag.CASTLE_Q)
                    } else if (c == -1 && sq == 4) {
                        if ((s.castle and 4) != 0 && b[5] == 0 && b[6] == 0 && b[7] == -R &&
                            !attacked(b, 4, 1) && !attacked(b, 5, 1) && !attacked(b, 6, 1)
                        ) out += ChessMove(4, 6, 0, ChessFlag.CASTLE_K)
                        if ((s.castle and 8) != 0 && b[3] == 0 && b[2] == 0 && b[1] == 0 && b[0] == -R &&
                            !attacked(b, 4, 1) && !attacked(b, 3, 1) && !attacked(b, 2, 1)
                        ) out += ChessMove(4, 2, 0, ChessFlag.CASTLE_Q)
                    }
                }
            } else {
                val d0 = if (t == B) 4 else 0
                val d1 = if (t == R) 4 else 8
                for (d in d0 until d1) {
                    for (to in RAYS[sq][d]) {
                        val q = b[to]
                        if (q == 0) {
                            if (!capturesOnly) out += ChessMove(sq, to)
                        } else {
                            if (q * c < 0) out += ChessMove(sq, to)
                            break
                        }
                    }
                }
            }
        }
        return out
    }

    /** Plays a move on a copy of the position. [track] keeps the repetition history (the search does not need it). */
    private fun apply(s: ChessPosition, m: ChessMove, track: Boolean): ChessPosition {
        val b = s.board.copyOf()
        val c = s.turn
        val p = b[m.from]
        val cap = b[m.to]
        var resets = Math.abs(p) == P || cap != 0
        b[m.to] = p
        b[m.from] = 0
        when (m.flag) {
            ChessFlag.EP -> {
                b[m.to + if (c == 1) 8 else -8] = 0
                resets = true
            }
            ChessFlag.CASTLE_K -> {
                b[m.to - 1] = b[m.to + 1]
                b[m.to + 1] = 0
            }
            ChessFlag.CASTLE_Q -> {
                b[m.to + 1] = b[m.to - 2]
                b[m.to - 2] = 0
            }
            else -> Unit
        }
        if (m.promo != 0) b[m.to] = c * m.promo
        val base = ChessPosition(
            b, -c, s.castle and CR[m.from] and CR[m.to],
            if (m.flag == ChessFlag.DOUBLE) (m.from + m.to) shr 1 else -1,
            if (resets) 0 else s.half + 1,
            s.full + if (c == -1) 1 else 0
        )
        return if (track) ChessPosition(b, base.turn, base.castle, base.ep, base.half, base.full, s.keys + positionKey(base))
        else base
    }

    private fun isLegal(s: ChessPosition, m: ChessMove): Boolean {
        val ns = apply(s, m, false)
        val k = findKing(ns.board, s.turn)
        return k >= 0 && !attacked(ns.board, k, -s.turn)
    }

    fun legalMoves(s: ChessPosition): List<ChessMove> = genMoves(s, false).filter { isLegal(s, it) }

    fun inCheck(s: ChessPosition): Boolean {
        val k = findKing(s.board, s.turn)
        return k >= 0 && attacked(s.board, k, -s.turn)
    }

    fun makeMove(s: ChessPosition, m: ChessMove): ChessPosition = apply(s, m, true)

    /** Number of move sequences of the given length; used to check the rules against published numbers. */
    fun perft(s: ChessPosition, depth: Int): Long {
        if (depth == 0) return 1
        val moves = legalMoves(s)
        if (depth == 1) return moves.size.toLong()
        var n = 0L
        for (m in moves) n += perft(apply(s, m, false), depth - 1)
        return n
    }

    // ---- Game state: who won, or why it is a draw ----

    private fun insufficientMaterial(b: IntArray): Boolean {
        val minors = mutableListOf<Pair<Int, Boolean>>()
        for (i in 0 until 64) {
            val t = Math.abs(b[i])
            if (t == 0 || t == K) continue
            if (t == P || t == R || t == Q) return false
            minors += t to ((((i shr 3) + (i and 7)) and 1) == 1)
        }
        if (minors.size <= 1) return true
        return minors.all { it.first == B && it.second == minors[0].second }
    }

    fun status(s: ChessPosition): ChessStatus {
        val check = inCheck(s)
        if (legalMoves(s).isEmpty()) {
            return if (check) ChessStatus(true, ChessResult.CHECKMATE, -s.turn, true)
            else ChessStatus(true, ChessResult.STALEMATE, 0, false)
        }
        if (insufficientMaterial(s.board)) return ChessStatus(true, ChessResult.MATERIAL, 0, check)
        if (s.half >= 100) return ChessStatus(true, ChessResult.FIFTY, 0, check)
        if (s.keys.isNotEmpty()) {
            val key = s.keys.last()
            if (s.keys.count { it == key } >= 3) return ChessStatus(true, ChessResult.REPETITION, 0, check)
        }
        return ChessStatus(false, null, 0, check)
    }

    /** Standard written form of a move (Nf3, exd5, O-O, e8=Q+). [legal] is legalMoves(s). */
    fun san(s: ChessPosition, m: ChessMove, legal: List<ChessMove> = legalMoves(s)): String {
        val p = Math.abs(s.board[m.from])
        val out = StringBuilder()
        if (m.flag == ChessFlag.CASTLE_K) out.append("O-O")
        else if (m.flag == ChessFlag.CASTLE_Q) out.append("O-O-O")
        else {
            val capture = s.board[m.to] != 0 || m.flag == ChessFlag.EP
            if (p == P) {
                if (capture) out.append(FILES[m.from and 7])
            } else {
                out.append("PNBRQK"[p - 1])
                val same = legal.filter { o -> o.to == m.to && o.from != m.from && Math.abs(s.board[o.from]) == p }
                if (same.isNotEmpty()) {
                    val sameFile = same.any { (it.from and 7) == (m.from and 7) }
                    val sameRank = same.any { (it.from shr 3) == (m.from shr 3) }
                    if (!sameFile) out.append(FILES[m.from and 7])
                    else if (!sameRank) out.append(8 - (m.from shr 3))
                    else out.append(sqName(m.from))
                }
            }
            if (capture) out.append('x')
            out.append(sqName(m.to))
            if (m.promo != 0) out.append('=').append("PNBRQK"[m.promo - 1])
        }
        val next = apply(s, m, false)
        if (inCheck(next)) out.append(if (legalMoves(next).isNotEmpty()) '+' else '#')
        return out.toString()
    }

    // ---- Computer opponent: material + piece position tables, alpha-beta search ----

    private val VAL = intArrayOf(0, 100, 320, 330, 500, 900, 0)
    private val PST: Array<IntArray> = arrayOf(
        IntArray(64),
        intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
        ),
        intArrayOf(
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
        ),
        intArrayOf(
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
        ),
        intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
        ),
        intArrayOf(
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
        ),
        intArrayOf(
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
        )
    )
    private val KING_END: IntArray = intArrayOf(
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
        )

    private const val MATE = 100000
    private const val INF = 1000000

    /** Score for the side to move, in centipawns (100 = one pawn). */
    fun evaluate(s: ChessPosition): Int {
        val b = s.board
        var score = 0
        var material = 0
        for (i in 0 until 64) {
            val p = b[i]
            val t = Math.abs(p)
            if (p != 0 && t != K && t != P) material += VAL[t]
        }
        val endgame = material < 2600
        for (i in 0 until 64) {
            val p = b[i]
            if (p == 0) continue
            val t = Math.abs(p)
            val white = p > 0
            val idx = if (white) i else i xor 56
            val v = if (t == K) 0 else VAL[t]
            val pos = if (t == K && endgame) KING_END[idx] else PST[t][idx]
            score += (if (white) 1 else -1) * (v + pos)
        }
        return score * s.turn
    }

    private fun orderMoves(s: ChessPosition, moves: List<ChessMove>): List<ChessMove> {
        val b = s.board
        fun order(m: ChessMove): Int {
            val victim = if (m.flag == ChessFlag.EP) P else Math.abs(b[m.to])
            var sc = 0
            if (victim != 0) sc = 10000 + 10 * VAL[victim] - VAL[Math.abs(b[m.from])]
            if (m.promo != 0) sc += VAL[m.promo]
            return sc
        }
        return moves.sortedByDescending(::order)
    }

    private class Search(val deadline: Long) {
        var nodes = 0
        var abort = false
    }

    private fun quiesce(s: ChessPosition, alphaIn: Int, beta: Int, depth: Int): Int {
        var alpha = alphaIn
        val stand = evaluate(s)
        if (stand >= beta) return beta
        if (stand > alpha) alpha = stand
        if (depth >= 4) return alpha
        val c = s.turn
        for (m in orderMoves(s, genMoves(s, true))) {
            val ns = apply(s, m, false)
            if (attacked(ns.board, findKing(ns.board, c), -c)) continue
            val sc = -quiesce(ns, -beta, -alpha, depth + 1)
            if (sc >= beta) return beta
            if (sc > alpha) alpha = sc
        }
        return alpha
    }

    private fun negamax(s: ChessPosition, depth: Int, alphaIn: Int, beta: Int, ply: Int, ctx: Search): Int {
        var alpha = alphaIn
        if ((++ctx.nodes and 1023) == 0 && ctx.deadline > 0 && System.currentTimeMillis() > ctx.deadline) ctx.abort = true
        if (ctx.abort) return 0
        if (depth <= 0) return quiesce(s, alpha, beta, 0)
        val c = s.turn
        var legal = 0
        var best = -INF
        for (m in orderMoves(s, genMoves(s, false))) {
            val ns = apply(s, m, false)
            if (attacked(ns.board, findKing(ns.board, c), -c)) continue
            legal++
            val sc = -negamax(ns, depth - 1, -beta, -alpha, ply + 1, ctx)
            if (ctx.abort) return 0
            if (sc > best) best = sc
            if (sc > alpha) alpha = sc
            if (alpha >= beta) break
        }
        if (legal == 0) return if (attacked(s.board, findKing(s.board, c), -c)) -MATE + ply else 0
        return best
    }

    /**
     * Easy looks one move ahead and sometimes picks a worse move; medium looks two moves ahead and is mostly sound;
     * hard searches as deep as time allows (up to 6 moves) and plays the best it finds.
     */
    fun bestMove(s: ChessPosition, level: ChessLevel, random: Random = Random.Default): ChessMove? {
        val moves = legalMoves(s).shuffled(random)
        if (moves.size <= 1) return moves.firstOrNull()

        if (level.timeMs == 0L) {
            val ctx = Search(0)
            val scored = moves.map { m ->
                m to -negamax(apply(s, m, false), level.depth - 1, -INF, INF, 1, ctx)
            }
            val top = scored.maxOf { it.second }
            val pool = scored.filter { it.second >= top - level.margin }
            return pool[random.nextInt(pool.size)].first
        }

        val ctx = Search(System.currentTimeMillis() + level.timeMs)
        var chosen = moves[0]
        var ordered = orderMoves(s, moves)
        for (depth in 1..level.depth) {
            var alpha = -INF
            var bestHere: ChessMove? = null
            var bestScore = -INF
            for (m in ordered) {
                val sc = -negamax(apply(s, m, false), depth - 1, -INF, -alpha, 1, ctx)
                if (ctx.abort) break
                if (sc > bestScore) {
                    bestScore = sc
                    bestHere = m
                }
                if (sc > alpha) alpha = sc
            }
            if (ctx.abort || bestHere == null) break
            chosen = bestHere
            if (bestScore > MATE - 100) break
            ordered = listOf(bestHere) + ordered.filter { it !== bestHere }
        }
        return chosen
    }
}
