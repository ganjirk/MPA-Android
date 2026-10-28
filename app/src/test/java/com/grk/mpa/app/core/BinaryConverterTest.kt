package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BinaryConverterTest {
    @Test
    fun decimalIntegerToBinary() {
        assertEquals("1010", BinaryConverter.decimalToBinary("10"))
        assertEquals("0", BinaryConverter.decimalToBinary("0"))
    }

    @Test
    fun decimalFractionToBinary() {
        assertEquals("1010.101", BinaryConverter.decimalToBinary("10.625"))
        assertEquals("0.1", BinaryConverter.decimalToBinary("0.5"))
    }

    @Test
    fun nonTerminatingFractionIsTruncated() {
        val result = BinaryConverter.decimalToBinary("0.1")!!
        assertEquals(BinaryConverter.MAX_FRACTION_DIGITS, result.substringAfter('.').length)
    }

    @Test
    fun binaryToDecimal() {
        assertEquals(10.0, BinaryConverter.binaryToDecimal("1010")!!, 0.0)
        assertEquals(10.625, BinaryConverter.binaryToDecimal("1010.101")!!, 0.0)
    }

    @Test
    fun negativeValuesKeepTheirSign() {
        assertEquals("-101", BinaryConverter.decimalToBinary("-5"))
        assertEquals(-5.0, BinaryConverter.binaryToDecimal("-101")!!, 0.0)
    }

    @Test
    fun invalidInputReturnsNull() {
        assertNull(BinaryConverter.decimalToBinary("abc"))
        assertNull(BinaryConverter.decimalToBinary(""))
        assertNull(BinaryConverter.decimalToBinary("1.2.3"))
        assertNull(BinaryConverter.binaryToDecimal("102"))
        assertNull(BinaryConverter.binaryToDecimal("1."))
    }

    @Test
    fun roundTripForExactValues() {
        val binary = BinaryConverter.decimalToBinary("37.75")!!
        assertEquals(37.75, BinaryConverter.binaryToDecimal(binary)!!, 0.0)
    }
}
