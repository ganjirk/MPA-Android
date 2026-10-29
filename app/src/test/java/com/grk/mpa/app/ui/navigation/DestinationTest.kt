package com.grk.mpa.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = Destination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun homeIsFirstTab() {
        assertEquals(Destination.HOME, Destination.entries.first())
    }

    @Test
    fun sectionsExcludeHomeAndKeepOrder() {
        assertFalse(Destination.HOME in Destination.sections)
        assertEquals(Destination.entries.size - 1, Destination.sections.size)
        assertEquals(Destination.entries.drop(1), Destination.sections)
    }

    @Test
    fun routesAreNotBlank() {
        assertTrue(Destination.entries.all { it.route.isNotBlank() })
    }
}
