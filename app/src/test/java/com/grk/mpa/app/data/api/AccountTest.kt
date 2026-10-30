package com.grk.mpa.app.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTest {
    @Test
    fun displayNameUsesFullNameWhenAvailable() {
        assertEquals("Ada Lovelace", Account(1, "ada", "Ada", "Lovelace").displayName)
        assertEquals("Ada", Account(1, "ada", firstName = "Ada").displayName)
        assertEquals("Lovelace", Account(1, "ada", lastName = "Lovelace").displayName)
    }

    @Test
    fun displayNameFallsBackToUsername() {
        assertEquals("ada", Account(1, "ada").displayName)
        assertEquals("ada", Account(1, "ada", firstName = "  ", lastName = "").displayName)
    }
}
