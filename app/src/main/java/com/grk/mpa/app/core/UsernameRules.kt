package com.grk.mpa.app.core

/**
 * The website's format rules for a username. Whether a name is already taken is only known by the
 * server (it ignores capitals), so that part is asked through the availability check.
 */
object UsernameRules {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 150

    private val allowed = Regex("^[\\p{L}\\p{M}\\p{N}_.@+-]+$")

    enum class Problem { TOO_SHORT, TOO_LONG, BAD_CHARACTERS }

    fun problem(value: String): Problem? {
        val name = value.trim()
        return when {
            name.length < MIN_LENGTH -> Problem.TOO_SHORT
            name.length > MAX_LENGTH -> Problem.TOO_LONG
            !allowed.matches(name) -> Problem.BAD_CHARACTERS
            else -> null
        }
    }
}
