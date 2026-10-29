package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.NumberGuessGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberGuessViewModelTest {
    private fun viewModel(target: Int = 25) =
        NumberGuessViewModel(NumberGuessGame(range = 1..50, initialTarget = target))

    @Test
    fun inputKeepsOnlyDigitsAndLimitsLength() {
        val vm = viewModel()
        vm.onInputChange("a1b2-3.4567")
        assertEquals("1234", vm.input)
    }

    @Test
    fun emptyInputIsInvalid() {
        val vm = viewModel()
        vm.submit()
        assertEquals(GuessFeedback.INVALID, vm.feedback)
        assertEquals(0, vm.attempts)
    }

    @Test
    fun hintsPointTowardsTheTarget() {
        val vm = viewModel(target = 25)
        vm.onInputChange("10")
        vm.submit()
        assertEquals(GuessFeedback.TOO_LOW, vm.feedback)
        vm.onInputChange("40")
        vm.submit()
        assertEquals(GuessFeedback.TOO_HIGH, vm.feedback)
        assertEquals(2, vm.attempts)
        assertFalse(vm.isSolved)
    }

    @Test
    fun outOfRangeGuessIsNotCounted() {
        val vm = viewModel()
        vm.onInputChange("0")
        vm.submit()
        assertEquals(GuessFeedback.OUT_OF_RANGE, vm.feedback)
        vm.onInputChange("51")
        vm.submit()
        assertEquals(GuessFeedback.OUT_OF_RANGE, vm.feedback)
        assertEquals(0, vm.attempts)
    }

    @Test
    fun correctGuessSolvesAndFurtherSubmitsAreIgnored() {
        val vm = viewModel(target = 25)
        vm.onInputChange("25")
        vm.submit()
        assertEquals(GuessFeedback.CORRECT, vm.feedback)
        assertTrue(vm.isSolved)
        assertEquals(1, vm.attempts)

        vm.onInputChange("3")
        vm.submit()
        assertEquals(GuessFeedback.CORRECT, vm.feedback)
        assertEquals(1, vm.attempts)
    }

    @Test
    fun resetClearsState() {
        val vm = viewModel(target = 25)
        vm.onInputChange("25")
        vm.submit()
        vm.reset()
        assertEquals("", vm.input)
        assertEquals(GuessFeedback.NONE, vm.feedback)
        assertEquals(0, vm.attempts)
        assertFalse(vm.isSolved)
    }
}
