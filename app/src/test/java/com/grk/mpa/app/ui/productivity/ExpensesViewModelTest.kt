package com.grk.mpa.app.ui.productivity

import com.grk.mpa.app.core.Expense
import com.grk.mpa.app.core.ExpenseCategory
import com.grk.mpa.app.core.ExpenseData
import com.grk.mpa.app.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

private class FakeExpenseRepository(var stored: ExpenseData = ExpenseData()) : ExpenseRepository {
    var saveCount = 0
    override suspend fun load(): ExpenseData = stored
    override suspend fun save(data: ExpenseData) {
        saveCount++
        stored = data
    }
}

class ExpensesViewModelTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val oct = YearMonth.of(2026, 10)

    private fun viewModel(repo: FakeExpenseRepository = FakeExpenseRepository()) =
        ExpensesViewModel(repo, CoroutineScope(Dispatchers.Unconfined)) { today }

    private fun ExpensesViewModel.addExpense(amount: String, description: String, category: ExpenseCategory = ExpenseCategory.FOOD) {
        onAmountChange(amount)
        onDescriptionChange(description)
        onCategoryChange(category)
        submit()
    }

    @Test
    fun startsOnTheCurrentMonthWithTodayAsTheDefaultDate() {
        val vm = viewModel()
        assertTrue(vm.isLoaded)
        assertEquals(oct, vm.viewedMonth)
        assertEquals(today, vm.spentOn)
        assertEquals(ExpenseCategory.OTHER, vm.category)
    }

    @Test
    fun addingAnExpenseSavesItAndResetsTheForm() {
        val repo = FakeExpenseRepository()
        val vm = viewModel(repo)
        vm.addExpense("120.50", "  Lunch ")
        assertEquals(listOf("Lunch"), vm.monthExpenses.map { it.description })
        assertEquals(12050L, vm.spentPaise)
        assertEquals("", vm.amountText)
        assertEquals("", vm.descriptionText)
        assertNull(vm.formError)
        assertEquals(vm.data, repo.stored)
    }

    @Test
    fun invalidFormsShowTheRightErrorAndSaveNothing() {
        val repo = FakeExpenseRepository()
        val vm = viewModel(repo)
        vm.addExpense("0", "Lunch")
        assertEquals(ExpenseFormError.AMOUNT, vm.formError)
        vm.onAmountChange("5")
        vm.onDescriptionChange("   ")
        vm.submit()
        assertEquals(ExpenseFormError.DESCRIPTION, vm.formError)
        vm.onDescriptionChange("Tea")
        vm.onDateChange(today.plusDays(1))
        vm.submit()
        assertEquals(ExpenseFormError.FUTURE_DATE, vm.formError)
        assertTrue(vm.data.expenses.isEmpty())
        assertEquals(0, repo.saveCount)
    }

    @Test
    fun monthNavigationStopsAtTheCurrentMonth() {
        val vm = viewModel()
        vm.nextMonth()
        assertEquals(oct, vm.viewedMonth)
        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.viewedMonth)
        assertFalse(vm.isCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 1), vm.spentOn)
        vm.nextMonth()
        assertEquals(oct, vm.viewedMonth)
        assertEquals(today, vm.spentOn)
    }

    @Test
    fun addingAnOlderExpenseShowsItsMonth() {
        val vm = viewModel()
        vm.onDateChange(LocalDate.of(2026, 8, 14))
        vm.addExpense("50", "Old bill")
        assertEquals(YearMonth.of(2026, 8), vm.viewedMonth)
        assertEquals(listOf("Old bill"), vm.monthExpenses.map { it.description })
    }

    @Test
    fun editingUpdatesTheExpenseAndCancelRestoresTheForm() {
        val vm = viewModel()
        vm.addExpense("100", "Snack")
        val id = vm.monthExpenses.single().id
        vm.startEdit(id)
        assertEquals(id, vm.editingId)
        assertEquals("100.00", vm.amountText)
        assertEquals("Snack", vm.descriptionText)
        vm.onAmountChange("150")
        vm.onCategoryChange(ExpenseCategory.HEALTH)
        vm.submit()
        val edited = vm.monthExpenses.single()
        assertEquals(15000L, edited.amountPaise)
        assertEquals(ExpenseCategory.HEALTH, edited.category)
        assertNull(vm.editingId)

        vm.startEdit(id)
        vm.cancelEdit()
        assertNull(vm.editingId)
        assertEquals("", vm.amountText)
    }

    @Test
    fun deletingRemovesTheExpenseAndLeavesEditMode() {
        val repo = FakeExpenseRepository()
        val vm = viewModel(repo)
        vm.addExpense("100", "Snack")
        val id = vm.monthExpenses.single().id
        vm.startEdit(id)
        vm.delete(id)
        assertTrue(vm.monthExpenses.isEmpty())
        assertNull(vm.editingId)
        assertTrue(repo.stored.expenses.isEmpty())
    }

    @Test
    fun budgetShowsRemainingAndOverBudget() {
        val vm = viewModel()
        assertNull(vm.remainingPaise)
        vm.onBudgetChange("1000")
        vm.saveBudget()
        assertEquals(100000L, vm.budgetPaise)
        assertEquals("1000.00", vm.budgetText)
        vm.addExpense("400", "Shoes", ExpenseCategory.SHOPPING)
        assertEquals(60000L, vm.remainingPaise)
        vm.addExpense("700", "Coat", ExpenseCategory.SHOPPING)
        assertEquals(-10000L, vm.remainingPaise)
    }

    @Test
    fun invalidBudgetIsFlaggedAndNotSaved() {
        val repo = FakeExpenseRepository()
        val vm = viewModel(repo)
        vm.onBudgetChange("abc")
        vm.saveBudget()
        assertTrue(vm.budgetError)
        assertNull(vm.budgetPaise)
        assertEquals(0, repo.saveCount)
        vm.onBudgetChange("10")
        assertFalse(vm.budgetError)
    }

    @Test
    fun budgetsAreKeptPerMonth() {
        val vm = viewModel()
        vm.onBudgetChange("1000")
        vm.saveBudget()
        vm.previousMonth()
        assertNull(vm.budgetPaise)
        assertEquals("", vm.budgetText)
        vm.previousMonth()
        vm.nextMonth()
        vm.nextMonth()
        assertEquals("1000.00", vm.budgetText)
    }

    @Test
    fun loadsStoredDataOnCreation() {
        val stored = ExpenseData(
            expenses = listOf(Expense(1, 2500, ExpenseCategory.HOME, "Rent share", LocalDate.of(2026, 10, 2))),
            budgets = mapOf(oct to 900000L)
        )
        val vm = viewModel(FakeExpenseRepository(stored))
        assertEquals(2500L, vm.spentPaise)
        assertEquals("9000.00", vm.budgetText)
        assertEquals(listOf(100), vm.breakdown.map { it.percent })
    }

    @Test
    fun exportUsesTheViewedMonth() {
        val vm = viewModel()
        vm.addExpense("10", "Pen")
        assertEquals("expenses-2026-10.csv", vm.csvFileName)
        assertTrue(vm.csvContent().contains("Pen"))
        vm.previousMonth()
        assertEquals("expenses-2026-09.csv", vm.csvFileName)
        assertFalse(vm.csvContent().contains("Pen"))
    }
}
