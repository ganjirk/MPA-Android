package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PongGameTest {
    private val config = PongConfig()

    @Test
    fun startsServedFromTheCentreTowardsTheRight() {
        val game = PongGame(config)
        assertEquals(500f, game.ballX, 0f)
        assertEquals(300f, game.ballY, 0f)
        assertEquals(config.serveSpeed, game.ballVx, 0f)
        assertEquals(0f, game.ballVy, 0f)
    }

    @Test
    fun ballTravelsInAStraightLineUntilSomethingIsHit() {
        val game = PongGame(config)
        game.step()
        assertEquals(500f + config.serveSpeed, game.ballX, 1e-4f)
        assertEquals(300f, game.ballY, 0f)
    }

    @Test
    fun paddleIsKeptInsideTheField() {
        val game = PongGame(config)
        game.movePaddle(PongSide.LEFT, -500f)
        assertEquals(config.paddleHeight / 2, game.leftPaddleY, 0f)
        game.movePaddle(PongSide.RIGHT, 5000f)
        assertEquals(config.height - config.paddleHeight / 2, game.rightPaddleY, 0f)
    }

    /** Runs until the ball first changes horizontal direction or [maxSteps] pass. */
    private fun stepUntilBounce(game: PongGame, maxSteps: Int = 2000) {
        val direction = game.ballVx
        var steps = 0
        while (game.ballVx * direction > 0 && steps++ < maxSteps) game.step()
    }

    @Test
    fun rightPaddleReturnsTheBallFaster() {
        val game = PongGame(config)
        game.movePaddle(PongSide.RIGHT, 300f)
        stepUntilBounce(game)
        assertTrue("ball should now move left", game.ballVx < 0)
        assertEquals(config.serveSpeed * config.speedUp, -game.ballVx, 1e-4f)
        assertEquals(0, game.leftScore)
        assertEquals(0, game.rightScore)
    }

    @Test
    fun hittingTheEdgeOfAPaddleDeflectsTheBall() {
        val game = PongGame(config)
        // Ball at y = 300; paddle centred 50 above it means the ball hits the lower half.
        game.movePaddle(PongSide.RIGHT, 250f)
        stepUntilBounce(game)
        assertTrue("ball should be deflected downwards (positive y)", game.ballVy > 0)
    }

    @Test
    fun ballDoesNotStickToAPaddleAfterBouncing() {
        val game = PongGame(config)
        game.movePaddle(PongSide.RIGHT, 300f)
        stepUntilBounce(game)
        val vxAfterBounce = game.ballVx
        repeat(5) { game.step() }
        assertEquals(vxAfterBounce, game.ballVx, 0f)
    }

    @Test
    fun missingTheBallScoresAndReServes() {
        val game = PongGame(config)
        game.movePaddle(PongSide.RIGHT, 50f) // far from the ball at y = 300
        repeat(130) { game.step() } // the ball leaves the field after 121 steps
        assertEquals(1, game.leftScore)
        assertEquals(0, game.rightScore)
        // Served back towards the left, as in the legacy game.
        assertTrue(game.ballVx < 0)
    }

    @Test
    fun leftPlayerMissingGivesRightPlayerAPoint() {
        val game = PongGame(config)
        game.movePaddle(PongSide.RIGHT, 300f)
        game.movePaddle(PongSide.LEFT, 550f) // away from the returning ball
        repeat(1000) { game.step() }
        assertTrue(game.rightScore >= 1)
    }

    @Test
    fun ballSpeedIsCappedAtMaxSpeed() {
        val game = PongGame(PongConfig(speedUp = 3f))
        game.movePaddle(PongSide.RIGHT, 300f)
        game.movePaddle(PongSide.LEFT, 300f)
        repeat(3000) {
            game.step()
            assertTrue(kotlin.math.abs(game.ballVx) <= 24f)
            assertTrue(kotlin.math.abs(game.ballVy) <= 24f)
        }
    }

    @Test
    fun ballBouncesOffTheTopAndBottomWalls() {
        // A short field and an off-centre paddle hit make the ball reach both walls.
        val short = PongConfig(height = 200f)
        val game = PongGame(short)
        game.movePaddle(PongSide.RIGHT, 150f) // clamped to 140, so the ball hits the upper part
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        repeat(1500) {
            game.step()
            minY = minOf(minY, game.ballY)
            maxY = maxOf(maxY, game.ballY)
        }
        // The ball touched, but never passed, each wall.
        assertEquals(short.ballSize / 2, minY, 1e-3f)
        assertEquals(short.height - short.ballSize / 2, maxY, 1e-3f)
    }

    @Test
    fun resetClearsScoresAndRecentresTheBall() {
        val game = PongGame(config)
        game.movePaddle(PongSide.RIGHT, 50f)
        repeat(130) { game.step() }
        game.reset()
        assertEquals(0, game.leftScore)
        assertEquals(0, game.rightScore)
        assertEquals(500f, game.ballX, 0f)
        assertEquals(300f, game.leftPaddleY, 0f)
    }
}

class FixedStepClockTest {
    private val step = 1_000_000_000L / 60

    @Test
    fun runsOneStepPerStepLength() {
        val clock = FixedStepClock()
        assertEquals(1, clock.advance(step))
        assertEquals(1, clock.advance(step))
    }

    @Test
    fun carriesRemainderOverBetweenFrames() {
        val clock = FixedStepClock()
        assertEquals(0, clock.advance(step / 2))
        assertEquals(1, clock.advance(step / 2))
    }

    @Test
    fun fastScreensRunFewerStepsPerFrame() {
        val clock = FixedStepClock()
        val total = (1..120).sumOf { clock.advance(1_000_000_000L / 120) }
        assertEquals(60, total)
    }

    @Test
    fun longStallsAreCappedAndBacklogDropped() {
        val clock = FixedStepClock(maxStepsPerFrame = 5)
        assertEquals(5, clock.advance(10_000_000_000L))
        assertEquals(0, clock.advance(0))
    }

    @Test
    fun negativeTimeIsIgnored() {
        val clock = FixedStepClock()
        assertEquals(0, clock.advance(-5))
    }
}
