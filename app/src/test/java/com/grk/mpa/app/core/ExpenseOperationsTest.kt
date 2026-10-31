package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseOperationsTest {
    private val oct = YearMonth.of(2026, 10)
    private fun date(m: Int, d: Int) = LocalDate.of(2026, m, d)

    private fun dataOf(vararg e: Expense) = ExpenseData(e.toList())

    @Test
    fun normalizeDescription() {
        assertEquals("Lunch with Sam", ExpenseOperations.normalizeDescription("  Lunch   with\nSam "))
        assertNull(ExpenseOperations.normalizeDescription("   "))
        assertEquals(120, ExpenseOperations.normalizeDescription("x".repeat(300))!!.length)
    }

    @Test
    fun addAssignsNextIdAndCleansDescription() {
        val first = ExpenseOperations.add(ExpenseData(), 1250, ExpenseCategory.FOOD, "  Lunch ", date(10, 3))
        assertEquals(Expense(1, 1250, ExpenseCategory.FOOD, "Lunch", date(10, 3)), first.expenses.single())
        val second = ExpenseOperations.add(first, 500, ExpenseCategory.OTHER, "Tea", date(10, 4))
        assertEquals(2L, second.expenses.last().id)
    }

    @Test
    fun addIgnoresInvalidInput() {
        val empty = ExpenseData()
        assertSame(empty, ExpenseOperations.add(empty, 0, ExpenseCategory.FOOD, "Lunch", date(10, 3)))
        assertSame(empty, ExpenseOperations.add(empty, 100, ExpenseCategory.FOOD, "  ", date(10, 3)))
        assertSame(empty, ExpenseOperations.add(empty, Money.MAX_PAISE + 1, ExpenseCategory.FOOD, "x", date(10, 3)))
    }

    @Test
    fun updateChangesOnlyTheMatchingExpense() {
        val data = dataOf(
            Expense(1, 100, ExpenseCategory.FOOD, "A", date(10, 1)),
            Expense(2, 200, ExpenseCategory.HOME, "B", date(10, 2))
        )
        val result = ExpenseOperations.update(data, 2, 999, ExpenseCategory.HEALTH, " Doctor ", date(10, 5))
        assertEquals(data.expenses[0], result.expenses[0])
        assertEquals(Expense(2, 999, ExpenseCategory.HEALTH, "Doctor", date(10, 5)), result.expenses[1])
    }

    @Test
    fun deleteRemovesTheExpense() {
        val data = dataOf(Expense(1, 100, ExpenseCategory.FOOD, "A", date(10, 1)), Expense(2, 200, ExpenseCategory.FOOD, "B", date(10, 2)))
        assertEquals(listOf(1L), ExpenseOperations.delete(data, 2).expenses.map { it.id })
    }

    @Test
    fun inMonthFiltersAndOrdersNewestFirst() {
        val list = listOf(
            Expense(1, 100, ExpenseCategory.FOOD, "old", date(9, 30)),
            Expense(2, 100, ExpenseCategory.FOOD, "first of month", date(10, 1)),
            Expense(3, 100, ExpenseCategory.FOOD, "mid", date(10, 15)),
            Expense(4, 100, ExpenseCategory.FOOD, "mid, added later", date(10, 15)),
            Expense(5, 100, ExpenseCategory.FOOD, "end", date(10, 31)),
            Expense(6, 100, ExpenseCategory.FOOD, "next month", date(11, 1))
        )
        assertEquals(listOf(5L, 4L, 3L, 2L), ExpenseOperations.inMonth(list, oct).map { it.id })
    }

    @Test
    fun breakdownIsLargestFirstWithPercentages() {
        val list = listOf(
            Expense(1, 3000, ExpenseCategory.FOOD, "a", date(10, 1)),
            Expense(2, 1000, ExpenseCategory.FOOD, "b", date(10, 2)),
            Expense(3, 4000, ExpenseCategory.TRANSPORT, "c", date(10, 3)),
            Expense(4, 2000, ExpenseCategory.HEALTH, "d", date(10, 4))
        )
        val rows = ExpenseOperations.breakdown(list)
        // Food and Transport are tied on 4000, so enum order breaks the tie (Food comes first).
        assertEquals(listOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT, ExpenseCategory.HEALTH), rows.map { it.category })
        assertEquals(listOf(40, 40, 20), rows.map { it.percent })
        assertEquals(10000L, ExpenseOperations.totalPaise(list))
    }

    @Test
    fun percentagesRoundHalvesToEvenLikePythonRound() {
        // 1 of 8 is 12.5% (rounds to 12) and 7 of 8 is 87.5% (rounds to 88).
        val list = listOf(
            Expense(1, 100, ExpenseCategory.FOOD, "a", date(10, 1)),
            Expense(2, 700, ExpenseCategory.HOME, "b", date(10, 2))
        )
        val percents = ExpenseOperations.breakdown(list).associate { it.category to it.percent }
        assertEquals(12, percents[ExpenseCategory.FOOD])
        assertEquals(88, percents[ExpenseCategory.HOME])
    }

    @Test
    fun breakdownOfNothingIsEmpty() {
        assertEquals(emptyList<CategoryTotal>(), ExpenseOperations.breakdown(emptyList()))
    }

    @Test
    fun budgetsAreSetPerMonthAndReplaced() {
        var data = ExpenseOperations.setBudget(ExpenseData(), oct, 500000)
        data = ExpenseOperations.setBudget(data, YearMonth.of(2026, 9), 300000)
        data = ExpenseOperations.setBudget(data, oct, 600000)
        assertEquals(600000L, data.budgets[oct])
        assertEquals(300000L, data.budgets[YearMonth.of(2026, 9)])
        assertSame(data, ExpenseOperations.setBudget(data, oct, 0))
    }

    @Test
    fun remainingIsNegativeWhenOverBudget() {
        assertEquals(1500L, ExpenseOperations.remainingPaise(5000, 3500))
        assertEquals(-250L, ExpenseOperations.remainingPaise(5000, 5250))
        assertNull(ExpenseOperations.remainingPaise(null, 100))
    }
    @Test
    fun updatingSyncedExpenseKeepsRemoteIdAndMarksPendingUpdate() {
        val original = Expense(2, 200, ExpenseCategory.HOME, "B", date(10, 2), 77L, ExpenseSyncState.SYNCED)
        val result = ExpenseOperations.update(ExpenseData(expenses = listOf(original)), 2, 999, ExpenseCategory.HEALTH, "Doctor", date(10, 5))
        assertEquals(77L, result.expenses.single().remoteId)
        assertEquals(ExpenseSyncState.PENDING_UPDATE, result.expenses.single().syncState)
        assertEquals("Doctor", result.expenses.single().description)
    }

    @Test
    fun deletingSyncedExpenseRetainsRemoteDeleteTombstone() {
        val synced = Expense(2, 200, ExpenseCategory.HOME, "B", date(10, 2), 77L, ExpenseSyncState.SYNCED)
        val result = ExpenseOperations.delete(ExpenseData(expenses = listOf(synced)), 2)
        assertEquals(emptyList<Expense>(), result.expenses)
        assertEquals(setOf(77L), result.pendingDeleteRemoteIds)
    }

    @Test
    fun deletingLocalOnlyExpenseDoesNotCreateRemoteTombstone() {
        val local = Expense(2, 200, ExpenseCategory.HOME, "B", date(10, 2))
        val result = ExpenseOperations.delete(ExpenseData(expenses = listOf(local)), 2)
        assertTrue(result.expenses.isEmpty())
        assertTrue(result.pendingDeleteRemoteIds.isEmpty())
    }

}
