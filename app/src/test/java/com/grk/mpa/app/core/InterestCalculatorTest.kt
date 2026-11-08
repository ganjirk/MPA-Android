package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestCalculatorTest {
    private fun input(
        principal: Double = 10_000.0, rate: Double = 12.0, unit: RateUnit = RateUnit.PERCENT_YEAR,
        years: Int = 1, months: Int = 0, days: Int = 0,
        type: InterestType = InterestType.SIMPLE, compounding: Compounding = Compounding.MONTHLY
    ) = InterestInput(principal, rate, unit, years, months, days, type, compounding)

    private fun ok(i: InterestInput) = (InterestCalculator.calculate(i) as InterestOutcome.Ok).result
    private fun error(i: InterestInput) = (InterestCalculator.calculate(i) as InterestOutcome.Invalid).error

    @Test
    fun simpleInterest() {
        assertEquals(1200.0, ok(input()).interest, 0.0)
        assertEquals(11_200.0, ok(input()).total, 0.0)
        assertEquals(600.0, ok(input(years = 0, months = 6)).interest, 0.0)
        assertEquals(1200.0, ok(input(years = 0, days = 365)).interest, 0.0)
        assertEquals(2700.0, ok(input(years = 2, months = 3)).interest, 0.0)
    }

    @Test
    fun rupeeInterestMeansRupeesPerHundredEachMonth() {
        val r = ok(input(rate = 2.0, unit = RateUnit.RS_PER_100_MONTH, years = 0, months = 12))
        assertEquals(24.0, r.annualPercent, 0.0)
        assertEquals(2.0, r.monthlyPercent, 0.0)
        assertEquals(2400.0, r.interest, 0.0)
        // Rs.1 interest on Rs.50,000 for 6 months is Rs.3,000
        assertEquals(3000.0, ok(input(50_000.0, 1.0, RateUnit.RS_PER_100_MONTH, 0, 6)).interest, 0.0)
        // Rs.2 interest is the same as 2% a month
        assertEquals(r.interest, ok(input(rate = 2.0, unit = RateUnit.PERCENT_MONTH, years = 0, months = 12)).interest, 0.0)
    }

    @Test
    fun compoundInterest() {
        assertEquals(12_544.0, ok(input(years = 2, type = InterestType.COMPOUND, compounding = Compounding.YEARLY)).total, 0.0)
        val monthly = ok(input(type = InterestType.COMPOUND))
        assertEquals(1268.25, monthly.interest, 0.0)
        assertEquals(12.6825, monthly.effectiveAnnualPercent, 0.001)
        assertTrue(ok(input(years = 5, type = InterestType.COMPOUND)).interest > ok(input(years = 5)).interest)
        assertEquals(0.0, ok(input(rate = 0.0)).interest, 0.0)
    }

    @Test
    fun yearByYearRows() {
        val rows = ok(input(years = 2, months = 6)).rows
        assertEquals(3, rows.size)
        assertEquals(1200.0, rows[0].interest, 0.0)
        assertEquals(true, rows[2].partial)
        assertEquals(3000.0, rows[2].interest, 0.0)
        assertEquals(1, ok(input()).rows.size)
        assertEquals(false, ok(input()).rows[0].partial)
    }

    @Test
    fun badInputGivesAnError() {
        assertEquals(InterestError.PRINCIPAL_INVALID, error(input(principal = 0.0)))
        assertEquals(InterestError.PRINCIPAL_INVALID, error(input(principal = -5.0)))
        assertEquals(InterestError.PRINCIPAL_INVALID, error(input(principal = Double.NaN)))
        assertEquals(InterestError.PRINCIPAL_TOO_LARGE, error(input(principal = 1e13)))
        assertEquals(InterestError.RATE_INVALID, error(input(rate = -1.0)))
        assertEquals(InterestError.RATE_TOO_HIGH, error(input(rate = 101.0, unit = RateUnit.PERCENT_MONTH)))
        assertEquals(InterestError.TIME_ZERO, error(input(years = 0)))
        assertEquals(InterestError.TIME_NEGATIVE, error(input(years = -1)))
        assertEquals(InterestError.TIME_TOO_LONG, error(input(years = 101)))
    }
}

class IndianMoneyTest {
    @Test
    fun groupsDigitsTheIndianWay() {
        assertEquals("₹0.00", IndianMoney.format(0.0))
        assertEquals("₹999.50", IndianMoney.format(999.5))
        assertEquals("₹1,000.00", IndianMoney.format(1000.0))
        assertEquals("₹12,345.67", IndianMoney.format(12345.67))
        assertEquals("₹1,23,456.00", IndianMoney.format(123456.0))
        assertEquals("₹12,34,567.00", IndianMoney.format(1234567.0))
        assertEquals("₹1,00,00,000.00", IndianMoney.format(10_000_000.0))
        assertEquals("-₹1,500.00", IndianMoney.format(-1500.0))
    }

    @Test
    fun percentDropsUselessZeros() {
        assertEquals("24%", IndianMoney.percent(24.0))
        assertEquals("12.68%", IndianMoney.percent(12.6825))
        assertEquals("0.5%", IndianMoney.percent(0.5))
        assertEquals("0%", IndianMoney.percent(0.0))
    }
}
