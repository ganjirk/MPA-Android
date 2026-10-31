package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Ported from the website's `Ludo/rules_check.js`. */
class LudoTest {
    private fun game(players: Int = 2) = Ludo.newGame(List(players) { LudoKind.HUMAN })

    private fun LudoState.withPos(seat: Int, vararg rel: Int) =
        copy(pos = pos.mapIndexed { i, row -> if (i == seat) rel.toList() else row })

    private fun LudoState.asMove(dice: Int) = copy(phase = LudoPhase.MOVE, dice = dice)

    private fun adjacent(a: LudoCell, b: LudoCell) = Math.abs(a.row - b.row) + Math.abs(a.col - b.col) == 1

    // ---- geometry ----

    @Test
    fun trackHas52DifferentSquaresThatTouchEdgeToEdge() {
        assertEquals(52, Ludo.TRACK.size)
        assertEquals(52, Ludo.TRACK.toSet().size)
        val corners = setOf(4, 17, 30, 43) // inner corners of the cross, where the path steps diagonally
        Ludo.TRACK.forEachIndexed { i, c ->
            val n = Ludo.TRACK[(i + 1) % 52]
            val diagonal = Math.abs(c.row - n.row) == 1 && Math.abs(c.col - n.col) == 1
            if (i in corners) assertTrue("square $i", diagonal) else assertTrue("square $i", adjacent(c, n))
        }
    }

    @Test
    fun homeColumnsAreSeparateContinuousAndReachTheCentre() {
        val used = Ludo.TRACK.toMutableSet()
        for (c in 0..3) {
            val col = Ludo.HOME[c]
            col.forEach { assertTrue("home column $c overlaps", used.add(it)) }
            for (i in 0..3) assertTrue(adjacent(col[i], col[i + 1]))
            val entry = Ludo.TRACK[(Ludo.START[c] + 50) % 52]
            assertTrue("colour $c enters its home column", adjacent(entry, col[0]))
            assertTrue(adjacent(col[4], Ludo.FINISH[c]))
            assertEquals(Ludo.TRACK[Ludo.START[c]], Ludo.cellOf(c, 0, 0))
            assertEquals(4, Ludo.BASE[c].toSet().size)
        }
        assertTrue(Ludo.STARS.none { it in Ludo.START })
        assertEquals(Ludo.START[3] + 13, 52)
    }

    @Test
    fun twoPlayersSitOpposite() {
        assertEquals(listOf(0, 2), Ludo.coloursFor(2))
        assertEquals(listOf(0, 1, 2), Ludo.coloursFor(3))
        assertEquals(listOf(0, 1, 2, 3), Ludo.coloursFor(4))
        assertEquals(listOf(0, 2), game(2).seats.map { it.color })
    }

    // ---- rules ----

    @Test
    fun leavingTheBaseNeedsASix() {
        val none = Ludo.rollDice(game(), 3)
        assertEquals(1, none.turn)
        assertTrue(none.events.any { it is LudoEvent.NoMove })

        val six = Ludo.rollDice(game(), 6)
        assertEquals(LudoPhase.MOVE, six.phase)
        assertEquals(listOf(0, 1, 2, 3), Ludo.legalTokens(six))

        val out = Ludo.moveToken(six, 0)
        assertEquals(0, out.pos[0][0])
        assertEquals(LudoPhase.ROLL, out.phase)
        assertEquals(0, out.turn)
    }

    @Test
    fun ordinaryMoveAdvancesAndPassesTheTurn() {
        val s = Ludo.moveToken(Ludo.rollDice(game().withPos(0, 5, -1, -1, -1), 4), 0)
        assertEquals(9, s.pos[0][0])
        assertEquals(1, s.turn)
        assertEquals(LudoPhase.ROLL, s.phase)
    }

    @Test
    fun thirdSixInARowLosesTheTurn() {
        var s = game().withPos(0, 5, -1, -1, -1)
        s = Ludo.moveToken(Ludo.rollDice(s, 6), 0)
        s = Ludo.moveToken(Ludo.rollDice(s, 6), 0)
        assertEquals(0, s.turn)
        assertEquals(2, s.sixes)
        s = Ludo.rollDice(s, 6)
        assertEquals(1, s.turn)
        assertTrue(s.events.any { it is LudoEvent.TripleSix })
        assertEquals(17, s.pos[0][0])
    }

    @Test
    fun exactRollIsNeededToGetHome() {
        val overshoot = Ludo.rollDice(game().withPos(0, 53, -1, -1, -1), 4)
        assertEquals(1, overshoot.turn)

        val exact = Ludo.rollDice(game().withPos(0, 53, -1, -1, -1), 3)
        assertEquals(listOf(0), Ludo.legalTokens(exact))
        val home = Ludo.moveToken(exact, 0)
        assertEquals(Ludo.HOME_REL, home.pos[0][0])
        assertTrue(home.events.any { it is LudoEvent.ReachedHome })
    }

    @Test
    fun allFourTokensHomeWins() {
        val s = Ludo.moveToken(Ludo.rollDice(game().withPos(0, 56, 56, 56, 55), 1), 3)
        assertEquals(LudoPhase.OVER, s.phase)
        assertEquals(0, s.winner)
        assertTrue(s.isOver)
        assertTrue(s.events.any { it is LudoEvent.Won })
    }

    @Test
    fun landingOnAnOpponentCapturesAndGivesAnotherRoll() {
        // red rel 3 is track square 3; yellow starts at 26, so yellow rel 29 is square 3 too
        val start = game().withPos(0, 1, -1, -1, -1).withPos(1, 29, -1, -1, -1)
        var s = Ludo.moveToken(Ludo.rollDice(start, 2), 0)
        assertEquals(-1, s.pos[1][0])
        assertTrue(s.events.any { it is LudoEvent.Captured })
        assertEquals(0, s.turn)
        assertEquals(LudoPhase.ROLL, s.phase)
        assertEquals(0, s.sixes)
        s = Ludo.rollDice(s, 1)
        assertTrue(s.phase == LudoPhase.MOVE || s.turn == 1)
    }

    @Test
    fun noCaptureOnStarOrStartSquares() {
        val star = Ludo.moveToken(
            Ludo.rollDice(game().withPos(0, 5, -1, -1, -1).withPos(1, 34, -1, -1, -1), 3), 0
        )
        assertEquals(34, star.pos[1][0])

        val start = Ludo.moveToken(
            Ludo.rollDice(game().withPos(0, 22, -1, -1, -1).withPos(1, 0, -1, -1, -1), 4), 0
        )
        assertEquals(0, start.pos[1][0])
    }

    @Test
    fun ownTokensShareAndHomeColumnIsPrivate() {
        val share = Ludo.moveToken(Ludo.rollDice(game().withPos(0, 1, 3, -1, -1), 2), 0)
        assertEquals(3, share.pos[0][0])
        assertEquals(3, share.pos[0][1])

        val column = Ludo.moveToken(Ludo.rollDice(game().withPos(0, 50, -1, -1, -1), 2), 0)
        assertEquals(52, column.pos[0][0])
        assertEquals(-1, Ludo.trackIndex(0, 52))
    }

    @Test
    fun captureSendsEveryOpponentOnTheSquareHome() {
        val s = Ludo.moveToken(
            Ludo.rollDice(game().withPos(0, 1, -1, -1, -1).withPos(1, 29, 29, -1, -1), 2), 0
        )
        assertEquals(-1, s.pos[1][0])
        assertEquals(-1, s.pos[1][1])
    }

    @Test
    fun illegalActionsChangeNothing() {
        val g = game()
        assertTrue(Ludo.moveToken(g, 0) === g)
        val rolled = Ludo.rollDice(g, 6)
        assertTrue(Ludo.rollDice(rolled, 2) === rolled)
        val s = Ludo.rollDice(game().withPos(0, 5, -1, -1, -1), 2)
        assertTrue(Ludo.moveToken(s, 1) === s) // base token with no 6
    }

    // ---- computer players ----

    @Test
    fun botBringsATokenOutOnASix() {
        val s = game().withPos(0, 10, 20, -1, -1).asMove(6)
        assertTrue(Ludo.chooseToken(s, LudoKind.CPU, Random(3)) in listOf(2, 3))
    }

    @Test
    fun botCapturesWhenItCan() {
        val s = game().withPos(0, 1, 12, -1, -1).withPos(1, 29, -1, -1, -1).asMove(2)
        assertEquals(0, Ludo.chooseToken(s, LudoKind.CPU, Random(5)))
    }

    @Test
    fun botGetsATokenHomeWhenItCan() {
        val s = game().withPos(0, 53, 10, -1, -1).asMove(3)
        assertEquals(0, Ludo.chooseToken(s, LudoKind.CPU, Random(7)))
    }

    @Test
    fun botReturnsMinusOneWithoutALegalMove() {
        assertEquals(-1, Ludo.chooseToken(game().asMove(3), LudoKind.CPU))
    }

    @Test
    fun easyBotStillPlaysLegalMoves() {
        val s = game().withPos(0, 10, 20, 30, -1).asMove(4)
        val random = Random(11)
        repeat(100) { assertTrue(Ludo.chooseToken(s, LudoKind.EASY, random) in Ludo.legalTokens(s)) }
    }

    // ---- layout ----

    @Test
    fun tokensInTheBaseAreSeparateAndFullSize() {
        val placements = LudoBoardLayout.placements(game())
        assertEquals(8, placements.size)
        assertTrue(placements.all { it.size == 0.78f })
    }

    @Test
    fun tokensSharingASquareAreShrunkAndSpreadOut() {
        val s = game().withPos(0, 5, 5, -1, -1)
        val shared = LudoBoardLayout.placements(s).filter { it.seat == 0 && it.token < 2 }
        assertTrue(shared.all { it.size < 0.78f })
        assertFalse(shared[0].x == shared[1].x && shared[0].y == shared[1].y)
    }

    // ---- whole games ----

    @Test
    fun randomGamesAlwaysFinishWithOneWinner() {
        for (n in 2..4) {
            val random = Random(n)
            repeat(100) {
                var s = Ludo.newGame(List(n) { if (it % 2 == 1) LudoKind.EASY else LudoKind.CPU })
                var plies = 0
                while (s.phase != LudoPhase.OVER && plies < 20_000) {
                    plies++
                    s = if (s.phase == LudoPhase.ROLL) Ludo.rollDice(s, Ludo.rollValue(random))
                    else Ludo.moveToken(s, Ludo.chooseToken(s, s.seats[s.turn].kind, random))
                    assertTrue(s.pos.all { row -> row.all { it in -1..Ludo.HOME_REL } })
                }
                assertEquals(LudoPhase.OVER, s.phase)
                assertTrue(s.pos[s.winner].all { it == Ludo.HOME_REL })
                s.pos.forEachIndexed { i, row -> if (i != s.winner) assertTrue(row.any { it != Ludo.HOME_REL }) }
            }
        }
    }
}
