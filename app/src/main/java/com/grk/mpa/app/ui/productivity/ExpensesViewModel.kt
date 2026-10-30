package com.grk.mpa.app.ui.productivity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.CategoryTotal
import com.grk.mpa.app.core.Expense
import com.grk.mpa.app.core.ExpenseCategory
import com.grk.mpa.app.core.ExpenseCsv
import com.grk.mpa.app.core.ExpenseData
import com.grk.mpa.app.core.ExpenseOperations
import com.grk.mpa.app.core.Money
import com.grk.mpa.app.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class ExpenseFormError { AMOUNT, DESCRIPTION, FUTURE_DATE }

/**
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 * @param today the current date; tests pass a fixed one.
 */
class ExpensesViewModel(
    private val repository: ExpenseRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope
    private val currentMonth: YearMonth get() = YearMonth.from(today())

    var data by mutableStateOf(ExpenseData())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var viewedMonth by mutableStateOf(YearMonth.from(today()))
        private set

    var amountText by mutableStateOf("")
        private set
    var descriptionText by mutableStateOf("")
        private set
    var category by mutableStateOf(ExpenseCategory.OTHER)
        private set
    var spentOn by mutableStateOf(today())
        private set
    var editingId by mutableStateOf<Long?>(null)
        private set
    var formError by mutableStateOf<ExpenseFormError?>(null)
        private set

    var budgetText by mutableStateOf("")
        private set
    var budgetError by mutableStateOf(false)
        private set

    val isCurrentMonth: Boolean get() = viewedMonth == currentMonth
    val monthExpenses: List<Expense> get() = ExpenseOperations.inMonth(data.expenses, viewedMonth)
    val spentPaise: Long get() = ExpenseOperations.totalPaise(monthExpenses)
    val breakdown: List<CategoryTotal> get() = ExpenseOperations.breakdown(monthExpenses)
    val budgetPaise: Long? get() = data.budgets[viewedMonth]
    val remainingPaise: Long? get() = ExpenseOperations.remainingPaise(budgetPaise, spentPaise)

    init {
        scope.launch {
            data = repository.load()
            isLoaded = true
            syncBudgetText()
        }
    }

    // --- month navigation -------------------------------------------------------------------

    fun previousMonth() = showMonth(viewedMonth.minusMonths(1))

    /** The website never shows months after the current one, so neither does the app. */
    fun nextMonth() {
        if (!isCurrentMonth) showMonth(viewedMonth.plusMonths(1))
    }

    fun goToCurrentMonth() = showMonth(currentMonth)

    private fun showMonth(month: YearMonth) {
        viewedMonth = if (month > currentMonth) currentMonth else month
        if (editingId == null) spentOn = defaultDate()
        formError = null
        budgetError = false
        syncBudgetText()
    }

    private fun defaultDate(): LocalDate = if (viewedMonth == currentMonth) today() else viewedMonth.atDay(1)

    // --- expense form -----------------------------------------------------------------------

    fun onAmountChange(text: String) {
        amountText = text.take(12)
    }

    fun onDescriptionChange(text: String) {
        descriptionText = text.take(ExpenseOperations.MAX_DESCRIPTION_LENGTH)
    }

    fun onCategoryChange(newCategory: ExpenseCategory) {
        category = newCategory
    }

    fun onDateChange(date: LocalDate) {
        spentOn = date
    }

    fun submit() {
        if (!isLoaded) return
        val amount = Money.parsePaise(amountText)
        val description = ExpenseOperations.normalizeDescription(descriptionText)
        when {
            amount == null -> formError = ExpenseFormError.AMOUNT
            description == null -> formError = ExpenseFormError.DESCRIPTION
            spentOn.isAfter(today()) -> formError = ExpenseFormError.FUTURE_DATE
            else -> {
                val id = editingId
                val newData = if (id == null) {
                    ExpenseOperations.add(data, amount, category, description, spentOn)
                } else {
                    ExpenseOperations.update(data, id, amount, category, description, spentOn)
                }
                save(newData)
                // Like the website, show the month the expense belongs to.
                viewedMonth = YearMonth.from(spentOn)
                resetForm()
            }
        }
    }

    fun startEdit(id: Long) {
        val expense = data.expenses.firstOrNull { it.id == id } ?: return
        editingId = id
        amountText = Money.formatAmount(expense.amountPaise)
        descriptionText = expense.description
        category = expense.category
        spentOn = expense.spentOn
        formError = null
    }

    fun cancelEdit() = resetForm()

    fun delete(id: Long) {
        if (!isLoaded) return
        save(ExpenseOperations.delete(data, id))
        if (editingId == id) resetForm()
    }

    private fun resetForm() {
        amountText = ""
        descriptionText = ""
        category = ExpenseCategory.OTHER
        editingId = null
        formError = null
        spentOn = defaultDate()
        syncBudgetText()
    }

    // --- budget -----------------------------------------------------------------------------

    fun onBudgetChange(text: String) {
        budgetText = text.take(12)
        budgetError = false
    }

    fun saveBudget() {
        if (!isLoaded) return
        val paise = Money.parsePaise(budgetText)
        if (paise == null) {
            budgetError = true
            return
        }
        save(ExpenseOperations.setBudget(data, viewedMonth, paise))
        budgetError = false
        syncBudgetText()
    }

    private fun syncBudgetText() {
        budgetText = budgetPaise?.let { Money.formatAmount(it) }.orEmpty()
    }

    // --- export -----------------------------------------------------------------------------

    val csvFileName: String get() = ExpenseCsv.fileName(viewedMonth)

    fun csvContent(): String = ExpenseCsv.build(viewedMonth, data.expenses)

    /** Shows the change immediately and saves it in the background. */
    private fun save(newData: ExpenseData) {
        data = newData
        scope.launch { repository.save(newData) }
    }
}
