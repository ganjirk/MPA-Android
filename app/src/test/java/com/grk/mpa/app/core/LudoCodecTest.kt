package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class LudoCodecTest {
    private fun midGame(): LudoState {
        val random = Random(42)
        var s = Ludo.newGame(listOf(LudoKind.HUMAN, LudoKind.CPU, LudoKind.EASY))
        repeat(80) {
            s = if (s.phase == LudoPhase.ROLL) Ludo.rollDice(s, Ludo.rollValue(random))
            else Ludo.moveToken(s, Ludo.chooseToken(s, LudoKind.CPU, random))
        }
        return s
    }

    @Test
    fun roundTripKeepsTheGame() {
        val s = midGame()
        assertEquals(s.copy(events = emptyList()), LudoCodec.decode(LudoCodec.encode(s)))
    }

    @Test
    fun finishedGameRoundTrips() {
        val g = Ludo.newGame(listOf(LudoKind.HUMAN, LudoKind.CPU))
        val won = Ludo.moveToken(Ludo.rollDice(g.copy(pos = listOf(listOf(56, 56, 56, 55), g.pos[1])), 1), 3)
        assertEquals(won.copy(events = emptyList()), LudoCodec.decode(LudoCodec.encode(won)))
    }

    @Test
    fun rejectsDamagedText() {
        val good = LudoCodec.encode(midGame())
        assertNotNull(LudoCodec.decode(good))
        assertNull(LudoCodec.decode(""))
        assertNull(LudoCodec.decode("hello"))
        assertNull(LudoCodec.decode(good.replace("LUDO1", "LUDO2")))
        assertNull(LudoCodec.decode(good.lines().take(3).joinToString("\n"))) // only one seat
        assertNull(LudoCodec.decode(good.replace("HUMAN", "ROBOT")))
        assertNull(LudoCodec.decode(good + "\nS 0 CPU 0 0 0 0")) // colours do not match four players
        assertNull(LudoCodec.decode(good.lines().first() + "\nT 9 0 0 ROLL -1 0\n" + good.lines().drop(2).joinToString("\n")))
    }

    @Test
    fun rejectsOutOfRangePositions() {
        val text = "LUDO1\nT 0 0 0 ROLL -1 0\nS 0 HUMAN 57 -1 -1 -1\nS 2 CPU -1 -1 -1 -1"
        assertNull(LudoCodec.decode(text))
        assertNotNull(LudoCodec.decode(text.replace("57", "56")))
    }

    @Test
    fun rejectsInconsistentWinner() {
        val base = "LUDO1\nT 0 1 0 OVER 0 5\nS 0 HUMAN 56 56 56 55\nS 2 CPU -1 -1 -1 -1"
        assertNull(LudoCodec.decode(base)) // winner has a token left
        assertNotNull(LudoCodec.decode(base.replace("55", "56")))
        assertNull(LudoCodec.decode("LUDO1\nT 0 1 0 ROLL 0 5\nS 0 HUMAN 1 1 1 1\nS 2 CPU -1 -1 -1 -1")) // winner while playing
    }
}
