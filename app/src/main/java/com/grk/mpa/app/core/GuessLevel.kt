package com.grk.mpa.app.core

import kotlin.random.Random

/** Difficulty levels of the website's Number Guess game: the bigger the range, the harder it is. */
enum class GuessLevel(val label: String, val maxNumber: Int, val chances: Int = DEFAULT_CHANCES) {
    NORMAL("Normal", 50),
    MEDIUM("Medium", 100),
    HARD("Hard", 200);

    fun newGame(random: Random = Random.Default) =
        NumberGuessGame(range = 1..maxNumber, random = random, maxAttempts = chances)

    companion object {
        const val DEFAULT_CHANCES = 5
        val DEFAULT = NORMAL
    }
}
