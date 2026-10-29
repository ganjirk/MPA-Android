package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LengthConverterTest {
    private val delta = 1e-9

    @Test
    fun metreToKilometre() {
        assertEquals(1.5, LengthConverter.convert(1500.0, LengthUnit.METRE, LengthUnit.KILOMETRE), delta)
    }

    @Test
    fun mileToMetre() {
        assertEquals(1609.344, LengthConverter.convert(1.0, LengthUnit.MILE, LengthUnit.METRE), delta)
    }

    @Test
    fun inchToCentimetre() {
        assertEquals(2.54, LengthConverter.convert(1.0, LengthUnit.INCH, LengthUnit.CENTIMETRE), delta)
    }

    @Test
    fun sameUnitIsIdentity() {
        LengthUnit.entries.forEach {
            assertEquals(42.0, LengthConverter.convert(42.0, it, it), delta)
        }
    }

    @Test
    fun zeroStaysZero() {
        LengthConverter.convertAll(0.0, LengthUnit.MILE).values.forEach { assertEquals(0.0, it, delta) }
    }

    @Test
    fun convertAllCoversEveryUnit() {
        val all = LengthConverter.convertAll(1.0, LengthUnit.METRE)
        assertEquals(LengthUnit.entries.size, all.size)
        assertEquals(1000.0, all.getValue(LengthUnit.MILLIMETRE), delta)
    }
}
