package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseCsvTest {
    private val oct = YearMonth.of(2026, 10)

    @Test
    fun fileNameMatchesTheWebsite() {
        assertEquals("expenses-2026-10.csv", ExpenseCsv.fileName(oct))
    }

    @Test
    fun buildsTheSameLayoutAsTheWebsite() {
        val expenses = listOf(
            Expense(2, 30000, ExpenseCategory.TRANSPORT, "Taxi, airport", LocalDate.of(2026, 10, 5)),
            Expense(1, 12050, ExpenseCategory.FOOD, "Lunch", LocalDate.of(2026, 10, 2)),
            Expense(3, 99900, ExpenseCategory.SHOPPING, "Other month", LocalDate.of(2026, 9, 30))
        )
        val expected = "﻿" +
            "Date,Description,Category,Amount (INR)\r\n" +
            "2026-10-02,Lunch,Food & dining,120.50\r\n" +
            "2026-10-05,\"Taxi, airport\",Transport,300.00\r\n" +
            "\r\n" +
            "Total,,,420.50\r\n"
        assertEquals(expected, ExpenseCsv.build(oct, expenses))
    }

    @Test
    fun emptyMonthHasZeroTotal() {
        val csv = ExpenseCsv.build(oct, emptyList())
        assertTrue(csv.endsWith("\r\nTotal,,,0.00\r\n"))
    }

    @Test
    fun formulaLikeTextIsNeutralised() {
        assertEquals("'=SUM(A1)", ExpenseCsv.csvSafe("=SUM(A1)"))
        assertEquals("'+1", ExpenseCsv.csvSafe("+1"))
        assertEquals("'-5 refund", ExpenseCsv.csvSafe("-5 refund"))
        assertEquals("'@home", ExpenseCsv.csvSafe("@home"))
        assertEquals("plain", ExpenseCsv.csvSafe("plain"))
        assertEquals("", ExpenseCsv.csvSafe(""))
    }

    @Test
    fun quotesAreDoubledInsideQuotedFields() {
        val expenses = listOf(Expense(1, 100, ExpenseCategory.OTHER, "The \"best\" pen", LocalDate.of(2026, 10, 1)))
        assertTrue(ExpenseCsv.build(oct, expenses).contains("\"The \"\"best\"\" pen\""))
    }
}
