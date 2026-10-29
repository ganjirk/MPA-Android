package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {
    @Test
    fun wholeNumbersHaveNoDecimalPoint() {
        assertEquals("1000", NumberFormatter.format(1000.0))
        assertEquals("42", NumberFormatter.format(42.0))
    }

    @Test
    fun zeroAndNegativeValues() {
        assertEquals("0", NumberFormatter.format(0.0))
        assertEquals("-2.5", NumberFormatter.format(-2.5))
    }

    @Test
    fun roundsToSignificantDigitsAndDropsTrailingZeros() {
        assertEquals("0.33333333", NumberFormatter.format(1.0 / 3))
        assertEquals("1.609344", NumberFormatter.format(1.609344))
        assertEquals("0.001", NumberFormatter.format(0.001))
    }

    @Test
    fun nonFiniteValuesShowDash() {
        assertEquals("-", NumberFormatter.format(Double.NaN))
        assertEquals("-", NumberFormatter.format(Double.POSITIVE_INFINITY))
    }

    @Test
    fun veryLargeValuesUseScientificNotation() {
        assertEquals("1E+20", NumberFormatter.format(1e20))
    }
}
