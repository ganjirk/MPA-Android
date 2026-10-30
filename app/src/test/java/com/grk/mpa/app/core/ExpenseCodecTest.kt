package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseCodecTest {
    @Test
    fun emptyDataRoundTrips() {
        assertEquals("", ExpenseCodec.encode(ExpenseData()))
        assertEquals(ExpenseData(), ExpenseCodec.decode(""))
    }

    @Test
    fun expensesAndBudgetsRoundTrip() {
        val data = ExpenseData(
            expenses = listOf(
                Expense(1, 1250, ExpenseCategory.FOOD, "Lunch", LocalDate.of(2026, 10, 3)),
                Expense(7, 99_999_999, ExpenseCategory.EDUCATION, "Course", LocalDate.of(2025, 1, 31))
            ),
            budgets = mapOf(YearMonth.of(2026, 10) to 500_000L, YearMonth.of(2026, 9) to 300_000L)
        )
        assertEquals(data, ExpenseCodec.decode(ExpenseCodec.encode(data)))
    }

    @Test
    fun specialCharactersInDescriptionsSurvive() {
        val tricky = "Tab\there, line\nbreak, 100% & +plus, ünïcödé, 日本語 😀"
        val data = ExpenseData(listOf(Expense(1, 100, ExpenseCategory.OTHER, tricky, LocalDate.of(2026, 1, 1))))
        val text = ExpenseCodec.encode(data)
        assertEquals(1, text.lines().size)
        assertEquals(data, ExpenseCodec.decode(text))
    }

    @Test
    fun unknownCategoryFallsBackToOther() {
        val text = "E\t1\t100\tspaceships\t2026-10-01\tToy"
        assertEquals(ExpenseCategory.OTHER, ExpenseCodec.decode(text).expenses.single().category)
    }

    @Test
    fun malformedLinesAreSkipped() {
        val good = "E\t1\t100\tfood\t2026-10-01\tKeep"
        val text = listOf(
            "garbage",
            "E\tx\t100\tfood\t2026-10-01\tBadId",
            "E\t2\t0\tfood\t2026-10-01\tZeroAmount",
            "E\t3\t100\tfood\tnot-a-date\tBadDate",
            "E\t4\t100\tfood\t2026-10-01\t",
            "E\t5\t100\tfood\t2026-10-01\t%ZZ",
            "E\t6\t100\tfood\t2026-10-01",
            "B\t2026-13\t100",
            "B\t2026-10\t-5",
            "B\t2026-10",
            good
        ).joinToString("\n")
        val result = ExpenseCodec.decode(text)
        assertEquals(listOf("Keep"), result.expenses.map { it.description })
        assertTrue(result.budgets.isEmpty())
    }
}
