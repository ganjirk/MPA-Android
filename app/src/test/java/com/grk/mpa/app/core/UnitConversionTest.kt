package com.grk.mpa.app.core

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConversionTest {
    private fun unit(category: UnitCategory, label: String) = category.units.first { it.label == label }

    private fun convert(value: Double, category: UnitCategory, from: String, to: String) =
        UnitConverter.convert(value, unit(category, from), unit(category, to))

    @Test
    fun categoriesAndUnitsMatchTheWebsite() {
        assertEquals(
            listOf("length", "time", "weight", "temperature", "volume", "speed", "area", "data"),
            UnitCategories.all.map { it.id }
        )
        assertEquals(
            mapOf(
                "length" to listOf("Feet", "Meters", "Inches", "cm", "Yards", "Kilometers", "Miles"),
                "time" to listOf("Seconds", "Minutes", "Hours", "Days", "Weeks"),
                "weight" to listOf("Pounds", "Kilograms", "Ounces", "Grams", "Stones"),
                "temperature" to listOf("Fahrenheit", "Celsius", "Kelvin"),
                "volume" to listOf("Litres", "Millilitres", "Gallons (US)", "Cups (US)", "Fl oz (US)"),
                "speed" to listOf("MPH", "KPH", "Knots", "Mach"),
                "area" to listOf("Sq metres", "Sq feet", "Acres", "Hectares", "Sq miles"),
                "data" to listOf("Bytes", "KB", "MB", "GB", "TB")
            ),
            UnitCategories.all.associate { category -> category.id to category.units.map { it.label } }
        )
    }

    @Test
    fun lengthConversions() {
        assertEquals(1609.344, convert(1.0, UnitCategories.length, "Miles", "Meters"), 1e-9)
        assertEquals(1.0, convert(12.0, UnitCategories.length, "Inches", "Feet"), 1e-9)
        assertEquals(2.54, convert(1.0, UnitCategories.length, "Inches", "cm"), 1e-9)
        assertEquals(3.0, convert(1.0, UnitCategories.length, "Yards", "Feet"), 1e-9)
    }

    @Test
    fun timeWeightAndDataConversions() {
        assertEquals(24.0, convert(1.0, UnitCategories.time, "Days", "Hours"), 1e-9)
        assertEquals(7.0, convert(1.0, UnitCategories.time, "Weeks", "Days"), 1e-9)
        assertEquals(14.0, convert(1.0, UnitCategories.weight, "Stones", "Pounds"), 1e-6)
        assertEquals(16.0, convert(1.0, UnitCategories.weight, "Pounds", "Ounces"), 1e-6)
        assertEquals(1000.0, convert(1.0, UnitCategories.data, "GB", "MB"), 1e-9)
    }

    @Test
    fun temperatureConversions() {
        assertEquals(212.0, convert(100.0, UnitCategories.temperature, "Celsius", "Fahrenheit"), 1e-9)
        assertEquals(0.0, convert(32.0, UnitCategories.temperature, "Fahrenheit", "Celsius"), 1e-9)
        assertEquals(-273.15, convert(0.0, UnitCategories.temperature, "Kelvin", "Celsius"), 1e-9)
        assertEquals(-40.0, convert(-40.0, UnitCategories.temperature, "Celsius", "Fahrenheit"), 1e-9)
    }

    @Test
    fun volumeSpeedAndAreaConversions() {
        assertEquals(128.0, convert(1.0, UnitCategories.volume, "Gallons (US)", "Fl oz (US)"), 1e-6)
        assertEquals(8.0, convert(1.0, UnitCategories.volume, "Cups (US)", "Fl oz (US)"), 1e-6)
        assertEquals(96.56064, convert(60.0, UnitCategories.speed, "MPH", "KPH"), 1e-6)
        assertEquals(1225.044, convert(1.0, UnitCategories.speed, "Mach", "KPH"), 1e-9)
        assertEquals(2.4710538, convert(1.0, UnitCategories.area, "Hectares", "Acres"), 1e-6)
    }

    @Test
    fun convertingToTheSameUnitChangesNothing() {
        UnitCategories.all.forEach { category ->
            category.units.forEach { unit ->
                assertEquals("${category.id}/${unit.label}", 42.5, UnitConverter.convert(42.5, unit, unit), 1e-9)
            }
        }
    }

    @Test
    fun everyPairOfUnitsRoundTrips() {
        val values = listOf(0.0, 1.0, 37.25, -12.5, 1234567.0)
        UnitCategories.all.forEach { category ->
            category.units.forEach { a ->
                category.units.forEach { b ->
                    values.forEach { x ->
                        val back = UnitConverter.convert(UnitConverter.convert(x, a, b), b, a)
                        val tolerance = 1e-9 * maxOf(1.0, abs(x))
                        assertTrue(
                            "${category.id}: $x ${a.label} -> ${b.label} -> back = $back",
                            abs(back - x) <= tolerance
                        )
                    }
                }
            }
        }
    }

    @Test
    fun onlyTheDataCategoryHasANote() {
        assertEquals(listOf("data"), UnitCategories.all.filter { it.note != null }.map { it.id })
    }
}
