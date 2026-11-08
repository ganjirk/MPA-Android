package com.grk.mpa.app.ui.utilities

import com.grk.mpa.app.core.InterestError
import com.grk.mpa.app.core.InterestOutcome
import com.grk.mpa.app.core.InterestType
import com.grk.mpa.app.core.RateUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestCalculatorViewModelTest {
    private fun filled() = InterestCalculatorViewModel().apply {
        onPrincipalChange("50000"); onRateChange("2"); onMonthsChange("6")
    }

    @Test
    fun startsEmptyWithRupeeInterestSelected() {
        val vm = InterestCalculatorViewModel()
        assertTrue(vm.isEmpty)
        assertNull(vm.outcome)
        assertEquals(RateUnit.RS_PER_100_MONTH, vm.rateUnit)
        assertEquals(InterestType.SIMPLE, vm.type)
    }

    @Test
    fun rupeeInterestExample() {
        val outcome = filled().outcome as InterestOutcome.Ok
        assertEquals(6000.0, outcome.result.interest, 0.0)
        assertEquals(56_000.0, outcome.result.total, 0.0)
    }

    @Test
    fun rateEquivalentFollowsTheChosenUnit() {
        val vm = filled()
        assertEquals(2.0 to 24.0, vm.rateEquivalent)
        vm.onRateUnitChange(RateUnit.PERCENT_YEAR)
        assertEquals(2.0 / 12 to 2.0, vm.rateEquivalent)
    }

    @Test
    fun partlyFilledFormShowsTheFirstProblem() {
        val vm = InterestCalculatorViewModel()
        vm.onPrincipalChange("1000")
        assertEquals(InterestError.RATE_INVALID, (vm.outcome as InterestOutcome.Invalid).error)
        vm.onRateChange("2")
        assertEquals(InterestError.TIME_ZERO, (vm.outcome as InterestOutcome.Invalid).error)
    }

    @Test
    fun inputsKeepOnlyAllowedCharacters() {
        val vm = InterestCalculatorViewModel()
        vm.onPrincipalChange("1a2,5.0-")
        assertEquals("12,5.0", vm.principal)
        vm.onYearsChange("1x2.5")
        assertEquals("125", vm.years)
        vm.onMonthsChange("12345678")
        assertEquals(4, vm.months.length)
    }

    @Test
    fun commaWorksAsDecimalPoint() {
        val vm = InterestCalculatorViewModel()
        vm.onPrincipalChange("1000,50"); vm.onRateChange("12"); vm.onRateUnitChange(RateUnit.PERCENT_YEAR); vm.onYearsChange("1")
        assertEquals(1000.5 * 0.12, (vm.outcome as InterestOutcome.Ok).result.interest, 0.01)
    }

    @Test
    fun clearEmptiesTheFormButKeepsTheChoices() {
        val vm = filled()
        vm.onTypeChange(InterestType.COMPOUND)
        vm.clear()
        assertTrue(vm.isEmpty)
        assertEquals(InterestType.COMPOUND, vm.type)
    }
}
