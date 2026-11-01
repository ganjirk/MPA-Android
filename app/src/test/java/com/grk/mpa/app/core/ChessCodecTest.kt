package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChessCodecTest {
    private val save = ChessSave(
        ChessMode.COMPUTER, -1, ChessLevel.HARD, true, 0,
        listOf(ChessMoveRef(52, 36), ChessMoveRef(12, 28), ChessMoveRef(62, 45))
    )

    @Test
    fun roundTrip() {
        assertEquals(save, ChessCodec.decode(ChessCodec.encode(save)))
    }

    @Test
    fun emptyGameAndResignationRoundTrip() {
        val fresh = ChessSave(ChessMode.TWO_PLAYERS, 1, ChessLevel.MEDIUM, false, 0, emptyList())
        assertEquals(fresh, ChessCodec.decode(ChessCodec.encode(fresh)))
        val resigned = save.copy(resignedLoser = -1)
        assertEquals(resigned, ChessCodec.decode(ChessCodec.encode(resigned)))
    }

    @Test
    fun rejectsDamagedText() {
        val good = ChessCodec.encode(save)
        assertNotNull(ChessCodec.decode(good))
        assertNull(ChessCodec.decode(""))
        assertNull(ChessCodec.decode("nope"))
        assertNull(ChessCodec.decode(good.replace("CHESS1", "CHESS2")))
        assertNull(ChessCodec.decode(good.replace("HARD", "INSANE")))
        assertNull(ChessCodec.decode(good.replace("COMPUTER", "ROBOT")))
        assertNull(ChessCodec.decode(good.replace("52-36-0", "99-36-0")))
        assertNull(ChessCodec.decode(good.replace("52-36-0", "52-36")))
        assertNull(ChessCodec.decode(good.replace("52-36-0", "a-b-c")))
        assertNull(ChessCodec.decode(good.lines().take(2).joinToString("\n")))
    }

    @Test
    fun replayPlaysLegalMovesAndRejectsIllegalOnes() {
        val ok = ChessGame.replay(listOf(ChessMoveRef(52, 36), ChessMoveRef(12, 28)))
        assertNotNull(ok)
        assertEquals(listOf("e4", "e5"), ok!!.second.map { it.san })
        assertEquals(1, ok.first.turn)
        assertNull(ChessGame.replay(listOf(ChessMoveRef(52, 20)))) // a pawn cannot jump four squares
        assertNull(ChessGame.replay(listOf(ChessMoveRef(12, 28)))) // black cannot move first
    }
}
