package com.grk.mpa.app.core

import kotlin.random.Random

enum class GuessResult { OUT_OF_RANGE, TOO_LOW, TOO_HIGH, CORRECT, LOST }

/**
 * Number guessing game ported from the legacy Kivy app and matched to the website's rules.
 *
 * - A guess outside [range] is rejected and does not use up an attempt.
 * - With [maxAttempts] set, running out of attempts loses the game ([isLost]); with it null, the
 *   game goes on until the number is found.
 * - Once the game is won or lost, further guesses repeat the final result.
 */
class NumberGuessGame(
    val range: IntRange = 1..50,
    private val random: Random = Random.Default,
    initialTarget: Int? = null,
    val maxAttempts: Int? = null
) {
    init {
        require(!range.isEmpty()) { "range must not be empty" }
        require(initialTarget == null || initialTarget in range) { "initialTarget must be in range" }
        require(maxAttempts == null || maxAttempts > 0) { "maxAttempts must be positive" }
    }

    private var target: Int = initialTarget ?: random.nextInt(range.first, range.last + 1)

    var attempts: Int = 0
        private set

    var isSolved: Boolean = false
        private set

    var isLost: Boolean = false
        private set

    /** Attempts remaining, or null when the number of attempts is not limited. */
    val attemptsLeft: Int? get() = maxAttempts?.let { it - attempts }

    /** The secret number, but only once the game is over. */
    val revealedTarget: Int? get() = if (isSolved || isLost) target else null

    fun guess(value: Int): GuessResult {
        if (isSolved) return GuessResult.CORRECT
        if (isLost) return GuessResult.LOST
        if (value !in range) return GuessResult.OUT_OF_RANGE
        attempts++
        return when {
            value == target -> GuessResult.CORRECT.also { isSolved = true }
            maxAttempts != null && attempts >= maxAttempts -> GuessResult.LOST.also { isLost = true }
            value > target -> GuessResult.TOO_HIGH
            else -> GuessResult.TOO_LOW
        }
    }

    fun reset() {
        target = random.nextInt(range.first, range.last + 1)
        attempts = 0
        isSolved = false
        isLost = false
    }
}
