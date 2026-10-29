package com.grk.mpa.app.ui.utilities

import com.grk.mpa.app.core.LengthUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LengthConverterViewModelTest {
    @Test
    fun emptyInputHasNoResultsAndIsNotAnError() {
        val vm = LengthConverterViewModel()
        assertTrue(vm.results.isEmpty())
        assertFalse(vm.isInvalid)
    }

    @Test
    fun convertsToEveryUnit() {
        val vm = LengthConverterViewModel()
        vm.onInputChange("1")
        vm.onUnitChange(LengthUnit.KILOMETRE)
        assertEquals(LengthUnit.entries.size, vm.results.size)
        assertEquals(1000.0, vm.results.first { it.first == LengthUnit.METRE }.second, 1e-9)
    }

    @Test
    fun acceptsCommaAsDecimalSeparator() {
        val vm = LengthConverterViewModel()
        vm.onInputChange("2,5")
        assertEquals(2.5, vm.value!!, 0.0)
    }

    @Test
    fun rejectsTextAndNonFiniteInput() {
        val vm = LengthConverterViewModel()
        for (bad in listOf("abc", "NaN", "Infinity", "1..2")) {
            vm.onInputChange(bad)
            assertNull(bad, vm.value)
            assertTrue(bad, vm.isInvalid)
            assertTrue(bad, vm.results.isEmpty())
        }
    }
}
