package com.grk.mpa.app.ui.utilities

import com.grk.mpa.app.core.UnitCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterViewModelTest {
    private fun unit(category: com.grk.mpa.app.core.UnitCategory, label: String) =
        category.units.first { it.label == label }

    @Test
    fun startsOnLengthWithEmptyFields() {
        val vm = UnitConverterViewModel()
        assertEquals(UnitCategories.length, vm.category)
        assertNull(vm.sourceUnit)
        assertTrue(vm.category.units.all { vm.displayValue(it) == "" })
        assertFalse(vm.isInvalid)
    }

    @Test
    fun typingInOneFieldFillsTheOthers() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        vm.onValueChange(unit(length, "Feet"), "3")
        assertEquals("3", vm.displayValue(unit(length, "Feet")))
        assertEquals("0.9144", vm.displayValue(unit(length, "Meters")))
        assertEquals("36", vm.displayValue(unit(length, "Inches")))
        assertEquals("91.44", vm.displayValue(unit(length, "cm")))
    }

    @Test
    fun theTypedTextIsKeptExactlyAsEntered() {
        val vm = UnitConverterViewModel()
        val feet = unit(UnitCategories.length, "Feet")
        vm.onValueChange(feet, "3.")
        assertEquals("3.", vm.displayValue(feet))
    }

    @Test
    fun commaIsAcceptedAsDecimalSeparator() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        vm.onValueChange(unit(length, "Meters"), "2,5")
        assertEquals("250", vm.displayValue(unit(length, "cm")))
    }

    @Test
    fun switchingCategoryClearsTheFields() {
        val vm = UnitConverterViewModel()
        vm.onValueChange(unit(UnitCategories.length, "Feet"), "3")
        vm.onCategoryChange(UnitCategories.temperature)
        assertEquals(UnitCategories.temperature, vm.category)
        assertNull(vm.sourceUnit)
        assertEquals("", vm.input)
        assertTrue(vm.category.units.all { vm.displayValue(it) == "" })
    }

    @Test
    fun temperatureConvertsBothWays() {
        val vm = UnitConverterViewModel()
        vm.onCategoryChange(UnitCategories.temperature)
        val temperature = UnitCategories.temperature
        vm.onValueChange(unit(temperature, "Celsius"), "100")
        assertEquals("212", vm.displayValue(unit(temperature, "Fahrenheit")))
        assertEquals("373.15", vm.displayValue(unit(temperature, "Kelvin")))
        vm.onValueChange(unit(temperature, "Fahrenheit"), "-40")
        assertEquals("-40", vm.displayValue(unit(temperature, "Celsius")))
    }

    @Test
    fun invalidTextIsFlaggedAndLeavesOtherFieldsEmpty() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        for (bad in listOf("abc", "NaN", "Infinity", "1..2")) {
            vm.onValueChange(unit(length, "Feet"), bad)
            assertTrue(bad, vm.isInvalid)
            assertEquals(bad, "", vm.displayValue(unit(length, "Meters")))
        }
    }

    @Test
    fun clearingTheFieldClearsTheOthers() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        vm.onValueChange(unit(length, "Feet"), "3")
        vm.onValueChange(unit(length, "Feet"), "")
        assertFalse(vm.isInvalid)
        assertEquals("", vm.displayValue(unit(length, "Meters")))
    }

    @Test
    fun typingInADifferentFieldMakesItTheSource() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        vm.onValueChange(unit(length, "Feet"), "3")
        vm.onValueChange(unit(length, "Inches"), "12")
        assertEquals("12", vm.displayValue(unit(length, "Inches")))
        assertEquals("1", vm.displayValue(unit(length, "Feet")))
    }
}
