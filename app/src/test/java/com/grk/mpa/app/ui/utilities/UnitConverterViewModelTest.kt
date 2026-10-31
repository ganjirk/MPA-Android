package com.grk.mpa.app.ui.utilities

import com.grk.mpa.app.core.UnitCategories
import com.grk.mpa.app.core.UnitCategory
import com.grk.mpa.app.core.ConversionUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterViewModelTest {

    private fun unit(category: UnitCategory, label: String): ConversionUnit =
        category.units.first { it.label == label }

    @Test
    fun startsWithEmptyFields() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length

        assertTrue(length.units.all { vm.displayValue(length, it).isEmpty() })
        assertFalse(vm.isInvalid(length))
    }

    @Test
    fun typingInOneFieldFillsTheOthers() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length

        vm.onValueChange(length, unit(length, "Feet"), "3")

        assertEquals("3", vm.displayValue(length, unit(length, "Feet")))
        assertEquals("0.9144", vm.displayValue(length, unit(length, "Meters")))
        assertEquals("36", vm.displayValue(length, unit(length, "Inches")))
        assertEquals("91.44", vm.displayValue(length, unit(length, "cm")))
    }

    @Test
    fun typedTextIsKeptExactlyAsEntered() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        val feet = unit(length, "Feet")

        vm.onValueChange(length, feet, "3.")

        assertEquals("3.", vm.displayValue(length, feet))
        assertTrue(vm.isSource(length, feet))
    }

    @Test
    fun commaIsAcceptedAsDecimalSeparator() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length

        vm.onValueChange(length, unit(length, "Meters"), "2,5")

        assertEquals("250", vm.displayValue(length, unit(length, "cm")))
        assertFalse(vm.isInvalid(length))
    }

    @Test
    fun valuesAreMaintainedIndependentlyForEachCategory() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        val temperature = UnitCategories.temperature

        vm.onValueChange(length, unit(length, "Feet"), "3")
        vm.onValueChange(temperature, unit(temperature, "Celsius"), "100")

        assertEquals("3", vm.displayValue(length, unit(length, "Feet")))
        assertEquals("0.9144", vm.displayValue(length, unit(length, "Meters")))
        assertEquals("100", vm.displayValue(temperature, unit(temperature, "Celsius")))
        assertEquals("212", vm.displayValue(temperature, unit(temperature, "Fahrenheit")))
    }

    @Test
    fun temperatureConvertsBothWays() {
        val vm = UnitConverterViewModel()
        val temperature = UnitCategories.temperature

        vm.onValueChange(temperature, unit(temperature, "Celsius"), "100")

        assertEquals("212", vm.displayValue(temperature, unit(temperature, "Fahrenheit")))
        assertEquals("373.15", vm.displayValue(temperature, unit(temperature, "Kelvin")))

        vm.onValueChange(temperature, unit(temperature, "Fahrenheit"), "-40")

        assertEquals("-40", vm.displayValue(temperature, unit(temperature, "Celsius")))
    }

    @Test
    fun invalidTextIsFlaggedAndLeavesOtherFieldsEmpty() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        val feet = unit(length, "Feet")
        val meters = unit(length, "Meters")

        for (bad in listOf("abc", "NaN", "Infinity", "1..2")) {
            vm.onValueChange(length, feet, bad)

            assertTrue("Expected invalid input: $bad", vm.isInvalid(length))
            assertEquals("", vm.displayValue(length, meters))
        }
    }

    @Test
    fun clearingTheFieldClearsTheOtherValues() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        val feet = unit(length, "Feet")

        vm.onValueChange(length, feet, "3")
        vm.onValueChange(length, feet, "")

        assertFalse(vm.isInvalid(length))
        assertEquals("", vm.displayValue(length, unit(length, "Meters")))
    }

    @Test
    fun typingInADifferentFieldMakesItTheSource() {
        val vm = UnitConverterViewModel()
        val length = UnitCategories.length
        val feet = unit(length, "Feet")
        val inches = unit(length, "Inches")

        vm.onValueChange(length, feet, "3")
        vm.onValueChange(length, inches, "12")

        assertTrue(vm.isSource(length, inches))
        assertFalse(vm.isSource(length, feet))
        assertEquals("12", vm.displayValue(length, inches))
        assertEquals("1", vm.displayValue(length, feet))
    }
}
