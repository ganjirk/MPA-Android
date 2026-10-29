package com.grk.mpa.app.core

import kotlin.math.abs

enum class PongSide { LEFT, RIGHT }

/** Field and physics settings, in abstract field units. y grows downwards. */
data class PongConfig(
    val width: Float = 1000f,
    val height: Float = 600f,
    val ballSize: Float = 40f,
    val paddleWidth: Float = 20f,
    val paddleHeight: Float = 120f,
    /** Horizontal ball speed after a serve, in units per step. */
    val serveSpeed: Float = 4f,
    /** Speed multiplier applied on every paddle hit. */
    val speedUp: Float = 1.1f,
    /** Upper bound for each velocity component, so the ball cannot tunnel through a paddle. */
    val maxSpeed: Float = 24f
)

/** An immutable view of the game, safe to hand to the UI. */
data class PongState(
    val config: PongConfig,
    val ballX: Float,
    val ballY: Float,
    val leftPaddleY: Float,
    val rightPaddleY: Float,
    val leftScore: Int,
    val rightScore: Int
)

/**
 * Two-player Pong, ported from the legacy Kivy game. Call [step] at a fixed rate (60 Hz in the app).
 *
 * Differences from the legacy game:
 * - A paddle only bounces a ball that is moving towards it, so the ball can no longer stick to a
 *   paddle by flipping direction every frame while they overlap.
 * - Ball speed is capped at [PongConfig.maxSpeed].
 * - Paddles are kept inside the field.
 */
class PongGame(val config: PongConfig = PongConfig()) {
    private val radius = config.ballSize / 2
    private val halfPaddle = config.paddleHeight / 2

    var ballX = config.width / 2
        private set
    var ballY = config.height / 2
        private set
    var ballVx = 0f
        private set
    var ballVy = 0f
        private set
    var leftPaddleY = config.height / 2
        private set
    var rightPaddleY = config.height / 2
        private set
    var leftScore = 0
        private set
    var rightScore = 0
        private set

    init {
        serve(toward = PongSide.RIGHT)
    }

    /** Puts the ball in the centre, moving horizontally towards [toward]. */
    fun serve(toward: PongSide) {
        ballX = config.width / 2
        ballY = config.height / 2
        ballVx = if (toward == PongSide.RIGHT) config.serveSpeed else -config.serveSpeed
        ballVy = 0f
    }

    fun movePaddle(side: PongSide, centerY: Float) {
        val clamped = centerY.coerceIn(halfPaddle, config.height - halfPaddle)
        when (side) {
            PongSide.LEFT -> leftPaddleY = clamped
            PongSide.RIGHT -> rightPaddleY = clamped
        }
    }

    fun step() {
        ballX += ballVx
        ballY += ballVy

        if (ballVx < 0 && touchesPaddle(paddleRight = config.paddleWidth, paddleY = leftPaddleY)) {
            bounce(leftPaddleY)
            ballX = config.paddleWidth + radius
        } else if (ballVx > 0 && touchesPaddle(paddleLeft = config.width - config.paddleWidth, paddleY = rightPaddleY)) {
            bounce(rightPaddleY)
            ballX = config.width - config.paddleWidth - radius
        }

        if (ballY - radius < 0) {
            ballY = radius
            ballVy = abs(ballVy)
        } else if (ballY + radius > config.height) {
            ballY = config.height - radius
            ballVy = -abs(ballVy)
        }

        // As in the legacy game, the ball is served towards the player who just scored.
        if (ballX - radius < 0) {
            rightScore++
            serve(toward = PongSide.RIGHT)
        } else if (ballX + radius > config.width) {
            leftScore++
            serve(toward = PongSide.LEFT)
        }
    }

    fun reset() {
        leftScore = 0
        rightScore = 0
        leftPaddleY = config.height / 2
        rightPaddleY = config.height / 2
        serve(toward = PongSide.RIGHT)
    }

    fun snapshot() = PongState(config, ballX, ballY, leftPaddleY, rightPaddleY, leftScore, rightScore)

    private fun touchesPaddle(
        paddleLeft: Float = 0f,
        paddleRight: Float = config.width,
        paddleY: Float
    ): Boolean =
        ballX + radius >= paddleLeft &&
            ballX - radius <= paddleRight &&
            abs(ballY - paddleY) <= halfPaddle + radius

    private fun bounce(paddleY: Float) {
        // Where the ball hit the paddle: -1 at the top edge, 0 in the middle, 1 at the bottom edge.
        val offset = ((ballY - paddleY) / halfPaddle).coerceIn(-1f, 1f)
        ballVx = (-ballVx * config.speedUp).coerceIn(-config.maxSpeed, config.maxSpeed)
        ballVy = (ballVy * config.speedUp + offset).coerceIn(-config.maxSpeed, config.maxSpeed)
    }
}

/**
 * Turns variable frame times into a whole number of fixed-length steps, so the game runs at the
 * same speed on 60 Hz and 120 Hz screens.
 */
class FixedStepClock(
    private val stepNanos: Long = 1_000_000_000L / 60,
    private val maxStepsPerFrame: Int = 5
) {
    private var accumulated = 0L

    /** Adds [elapsedNanos] and returns how many steps to run now. */
    fun advance(elapsedNanos: Long): Int {
        accumulated += elapsedNanos.coerceAtLeast(0)
        val steps = (accumulated / stepNanos).toInt()
        accumulated -= steps * stepNanos
        if (steps > maxStepsPerFrame) {
            // The app was paused or stalled; drop the backlog instead of fast-forwarding.
            accumulated = 0
            return maxStepsPerFrame
        }
        return steps
    }
}
