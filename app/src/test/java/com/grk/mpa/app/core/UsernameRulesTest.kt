package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsernameRulesTest {
    @Test
    fun goodNamesPass() {
        listOf("abc", "Asha_K", "a.b-c+d@e", "user123", "ಕನ್ನಡ", "  padded  ").forEach {
            assertNull(it, UsernameRules.problem(it))
        }
    }

    @Test
    fun tooShortOrBlank() {
        assertEquals(UsernameRules.Problem.TOO_SHORT, UsernameRules.problem("ab"))
        assertEquals(UsernameRules.Problem.TOO_SHORT, UsernameRules.problem("   "))
        assertEquals(UsernameRules.Problem.TOO_SHORT, UsernameRules.problem(" a "))
    }

    @Test
    fun tooLong() {
        assertNull(UsernameRules.problem("a".repeat(150)))
        assertEquals(UsernameRules.Problem.TOO_LONG, UsernameRules.problem("a".repeat(151)))
    }

    @Test
    fun badCharacters() {
        listOf("has space", "semi;colon", "slash/name", "emoji😀x", "a!b").forEach {
            assertEquals(it, UsernameRules.Problem.BAD_CHARACTERS, UsernameRules.problem(it))
        }
    }
}
