package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChessTest {
    private fun sq(name: String): Int = (8 - (name[1] - '0')) * 8 + "abcdefgh".indexOf(name[0])

    private fun find(p: ChessPosition, from: String, to: String, promo: Int = 0): ChessMove =
        Chess.legalMoves(p).first { it.from == sq(from) && it.to == sq(to) && it.promo == promo }

    private fun play(p: ChessPosition, vararg moves: String): ChessPosition =
        moves.fold(p) { pos, m -> Chess.makeMove(pos, find(pos, m.substring(0, 2), m.substring(2, 4))) }

    // ---- the published move counts (perft), as in the website's Chess/perft.js ----

    @Test
    fun startPositionMoveCounts() {
        val s = Chess.newGame()
        assertEquals(listOf(20L, 400L, 8902L, 197281L), (1..4).map { Chess.perft(s, it) })
    }

    @Test
    fun kiwipeteMoveCounts() {
        val s = Chess.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(listOf(48L, 2039L, 97862L), (1..3).map { Chess.perft(s, it) })
    }

    @Test
    fun endgameMoveCounts() {
        val s = Chess.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
        assertEquals(listOf(14L, 191L, 2812L, 43238L), (1..4).map { Chess.perft(s, it) })
    }

    @Test
    fun promotionMoveCounts() {
        val s = Chess.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")
        assertEquals(listOf(6L, 264L, 9467L), (1..3).map { Chess.perft(s, it) })
    }

    @Test
    fun position5MoveCounts() {
        val s = Chess.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8")
        assertEquals(listOf(44L, 1486L, 62379L), (1..3).map { Chess.perft(s, it) })
    }

    // ---- positions ----

    @Test
    fun fenRoundTrips() {
        val fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        assertEquals(fen, Chess.toFen(Chess.fromFen(fen)))
        assertEquals(Chess.START_FEN, Chess.toFen(Chess.newGame()))
        assertEquals("a1", Chess.sqName(56))
        assertEquals("h8", Chess.sqName(7))
    }

    // ---- game end ----

    @Test
    fun foolsMateIsCheckmateForBlack() {
        val s = play(Chess.newGame(), "f2f3", "e7e5", "g2g4", "d8h4")
        val status = Chess.status(s)
        assertTrue(status.over)
        assertEquals(ChessResult.CHECKMATE, status.result)
        assertEquals(-1, status.winner)
        assertTrue(status.check)
    }

    @Test
    fun stalemateIsADraw() {
        val status = Chess.status(Chess.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"))
        assertEquals(ChessResult.STALEMATE, status.result)
        assertEquals(0, status.winner)
    }

    @Test
    fun bareKingsAreADraw() {
        assertEquals(ChessResult.MATERIAL, Chess.status(Chess.fromFen("8/8/8/4k3/8/8/3K4/8 w - - 0 1")).result)
        assertEquals(ChessResult.MATERIAL, Chess.status(Chess.fromFen("8/8/8/4k3/8/8/3K4/5N2 w - - 0 1")).result)
        assertFalse(Chess.status(Chess.fromFen("8/8/8/4k3/8/8/3K4/R7 w - - 0 1")).over)
    }

    @Test
    fun fiftyMoveRule() {
        assertEquals(ChessResult.FIFTY, Chess.status(Chess.fromFen("8/8/8/4k3/8/8/3K4/R7 w - - 100 80")).result)
        assertFalse(Chess.status(Chess.fromFen("8/8/8/4k3/8/8/3K4/R7 w - - 99 80")).over)
    }

    @Test
    fun threefoldRepetition() {
        var s = Chess.newGame()
        val shuffle = arrayOf("g1f3", "g8f6", "f3g1", "f6g8")
        s = play(s, *shuffle)
        assertFalse(Chess.status(s).over)
        s = play(s, *shuffle)
        val status = Chess.status(s)
        assertEquals(ChessResult.REPETITION, status.result)
    }

    // ---- special moves and notation ----

    @Test
    fun castlingBothWays() {
        val s = Chess.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val short = Chess.legalMoves(s).first { it.flag == ChessFlag.CASTLE_K }
        val long = Chess.legalMoves(s).first { it.flag == ChessFlag.CASTLE_Q }
        assertEquals("O-O", Chess.san(s, short))
        assertEquals("O-O-O", Chess.san(s, long))
        val after = Chess.makeMove(s, short)
        assertEquals(Chess.K, after.board[sq("g1")])
        assertEquals(Chess.R, after.board[sq("f1")])
        assertEquals(0, after.board[sq("h1")])
    }

    @Test
    fun cannotCastleThroughCheck() {
        val s = Chess.fromFen("4k3/8/8/8/8/5r2/8/R3K2R w KQ - 0 1")
        assertTrue(Chess.legalMoves(s).none { it.flag == ChessFlag.CASTLE_K })
        assertTrue(Chess.legalMoves(s).any { it.flag == ChessFlag.CASTLE_Q })
    }

    @Test
    fun enPassantCapturesThePawn() {
        val s = Chess.fromFen("rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 3")
        val ep = Chess.legalMoves(s).first { it.flag == ChessFlag.EP }
        assertEquals(sq("e3"), ep.to)
        assertEquals("dxe3", Chess.san(s, ep))
        val after = Chess.makeMove(s, ep)
        assertEquals(0, after.board[sq("e4")])
        assertEquals(-Chess.P, after.board[sq("e3")])
        assertEquals(Chess.P, ChessGame.entry(s, ep, Chess.legalMoves(s)).captured)
    }

    @Test
    fun promotionOffersFourPieces() {
        val s = Chess.fromFen("8/P6k/8/8/8/8/8/K7 w - - 0 1")
        val promos = Chess.legalMoves(s).filter { it.from == sq("a7") }
        assertEquals(setOf(Chess.Q, Chess.R, Chess.B, Chess.N), promos.map { it.promo }.toSet())
        val queen = promos.first { it.promo == Chess.Q }
        assertEquals("a8=Q", Chess.san(s, queen))
        assertEquals(Chess.Q, Chess.makeMove(s, queen).board[sq("a8")])
    }

    @Test
    fun notationAddsFileWhenTwoKnightsCanGo() {
        val s = Chess.fromFen("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1")
        assertEquals("Nbd2", Chess.san(s, find(s, "b1", "d2")))
        assertEquals("Nfd2", Chess.san(s, find(s, "f1", "d2")))
    }

    @Test
    fun notationMarksCheckAndMate() {
        val s = Chess.fromFen("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1")
        assertEquals("Ra8#", Chess.san(s, find(s, "a1", "a8")))
        assertEquals("Ra7", Chess.san(s, find(s, "a1", "a7")))
    }

    // ---- computer ----

    @Test
    fun computerFindsMateInOne() {
        val s = Chess.fromFen("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1")
        val m = Chess.bestMove(s, ChessLevel.MEDIUM, Random(1))
        assertEquals(sq("a8"), m?.to)
    }

    @Test
    fun computerAlwaysPlaysLegalMoves() {
        val random = Random(5)
        var s = Chess.newGame()
        repeat(40) {
            if (Chess.status(s).over) return
            val level = if (it % 2 == 0) ChessLevel.EASY else ChessLevel.MEDIUM
            val m = Chess.bestMove(s, level, random)
            assertNotNull(m)
            assertTrue(Chess.legalMoves(s).contains(m))
            s = Chess.makeMove(s, m!!)
        }
    }

    @Test
    fun computerHasNoMoveWhenGameIsOver() {
        val s = Chess.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertNull(Chess.bestMove(s, ChessLevel.EASY))
    }

    @Test
    fun evaluationFavoursTheSideWithMoreMaterial() {
        val up = Chess.fromFen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
        val down = Chess.fromFen("4k3/8/8/8/8/8/8/3QK3 b - - 0 1")
        assertTrue(Chess.evaluate(up) > 800)
        assertTrue(Chess.evaluate(down) < -800)
    }
}
