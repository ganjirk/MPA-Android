package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesWholeAndDecimalAmounts() {
        assertEquals(1200L, Money.parsePaise("12"))
        assertEquals(1250L, Money.parsePaise("12.5"))
        assertEquals(1250L, Money.parsePaise("12.50"))
        assertEquals(1L, Money.parsePaise("0.01"))
        assertEquals(50L, Money.parsePaise(".5"))
        assertEquals(1250L, Money.parsePaise("  12,50 "))
    }

    @Test
    fun acceptsTheLargestAmountTheWebsiteAllows() {
        assertEquals(Money.MAX_PAISE, Money.parsePaise("99999999.99"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        for (bad in listOf("", " ", ".", "0", "0.00", "-5", "1.234", "abc", "1e5", "12.", "123456789", "1.2.3", "₹5")) {
            assertNull("'$bad' should be rejected", Money.parsePaise(bad))
        }
    }

    @Test
    fun formatsWithTwoDecimalsAndNoSeparators() {
        assertEquals("12.50", Money.formatAmount(1250))
        assertEquals("0.05", Money.formatAmount(5))
        assertEquals("1234567.00", Money.formatAmount(123_456_700))
        assertEquals("-0.50", Money.formatAmount(-50))
        assertEquals("₹12.50", Money.format(1250))
    }
}
