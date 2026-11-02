package com.grk.mpa.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchTest {
    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(HomeSearch.matches("", "Games"))
        assertTrue(HomeSearch.matches("   ", "Games", "Play"))
    }

    @Test
    fun ignoresCaseAndSearchesAllFields() {
        assertTrue(HomeSearch.matches("GAM", "Games", "Play something"))
        assertTrue(HomeSearch.matches("play", "Games", "Play something"))
    }

    @Test
    fun everyWordMustMatch() {
        assertTrue(HomeSearch.matches("play games", "Games", "Play something"))
        assertFalse(HomeSearch.matches("play chess", "Games", "Play something"))
    }

    @Test
    fun noMatch() {
        assertFalse(HomeSearch.matches("zzz", "Games", "Play"))
    }
}
