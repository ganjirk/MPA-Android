package com.grk.mpa.app.core

import kotlin.random.Random

enum class GuessResult { OUT_OF_RANGE, TOO_LOW, TOO_HIGH, CORRECT }

/**
 * Number guessing game ported from the legacy Kivy app. The legacy reset logic never
 * actually replaced the secret number (it assigned a local variable); [reset] here does.
 */
class NumberGuessGame(
    val range: IntRange = 1..50,
    private val random: Random = Random.Default,
    initialTarget: Int? = null
) {
    init {
        require(!range.isEmpty()) { "range must not be empty" }
        require(initialTarget == null || initialTarget in range) { "initialTarget must be in range" }
    }

    private var target: Int = initialTarget ?: random.nextInt(range.first, range.last + 1)

    var attempts: Int = 0
        private set

    var isSolved: Boolean = false
        private set

    fun guess(value: Int): GuessResult {
        if (isSolved) return GuessResult.CORRECT
        if (value !in range) return GuessResult.OUT_OF_RANGE
        attempts++
        return when {
            value == target -> GuessResult.CORRECT.also { isSolved = true }
            value > target -> GuessResult.TOO_HIGH
            else -> GuessResult.TOO_LOW
        }
    }

    fun reset() {
        target = random.nextInt(range.first, range.last + 1)
        attempts = 0
        isSolved = false
    }
}
