package com.grk.mpa.app.core

import java.time.LocalDate
import java.time.YearMonth

/** Pure functions on [ExpenseData]. Invalid input leaves the data unchanged. */
object ExpenseOperations {
    const val MAX_DESCRIPTION_LENGTH = 120

    fun normalizeDescription(raw: String): String? =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_DESCRIPTION_LENGTH).trim().takeIf { it.isNotEmpty() }

    fun add(
        data: ExpenseData,
        amountPaise: Long,
        category: ExpenseCategory,
        rawDescription: String,
        spentOn: LocalDate
    ): ExpenseData {
        val description = normalizeDescription(rawDescription)
        if (description == null || amountPaise !in 1..Money.MAX_PAISE) return data
        val nextId = (data.expenses.maxOfOrNull { it.id } ?: 0L) + 1
        return data.copy(expenses = data.expenses + Expense(nextId, amountPaise, category, description, spentOn))
    }

    fun update(
        data: ExpenseData,
        id: Long,
        amountPaise: Long,
        category: ExpenseCategory,
        rawDescription: String,
        spentOn: LocalDate
    ): ExpenseData {
        val description = normalizeDescription(rawDescription)
        if (description == null || amountPaise !in 1..Money.MAX_PAISE) return data
        return data.copy(
            expenses = data.expenses.map { expense ->
                if (expense.id != id) expense
                else {
                    val nextState = when (expense.syncState) {
                        ExpenseSyncState.LOCAL_ONLY -> ExpenseSyncState.LOCAL_ONLY
                        ExpenseSyncState.PENDING_CREATE -> ExpenseSyncState.PENDING_CREATE
                        ExpenseSyncState.SYNCED, ExpenseSyncState.PENDING_UPDATE -> ExpenseSyncState.PENDING_UPDATE
                    }
                    expense.copy(
                        amountPaise = amountPaise,
                        category = category,
                        description = description,
                        spentOn = spentOn,
                        syncState = nextState
                    )
                }
            }
        )
    }

    fun delete(data: ExpenseData, id: Long): ExpenseData {
        val target = data.expenses.firstOrNull { it.id == id } ?: return data
        val remaining = data.expenses.filterNot { it.id == id }
        val tombstones = if (target.remoteId != null &&
            target.syncState in setOf(ExpenseSyncState.SYNCED, ExpenseSyncState.PENDING_UPDATE)
        ) data.pendingDeleteRemoteIds + target.remoteId else data.pendingDeleteRemoteIds
        return data.copy(expenses = remaining, pendingDeleteRemoteIds = tombstones)
    }

    /** Expenses in [month], newest first (ties: most recently added first), as on the website. */
    fun inMonth(expenses: List<Expense>, month: YearMonth): List<Expense> =
        expenses.filter { YearMonth.from(it.spentOn) == month }
            .sortedWith(compareByDescending<Expense> { it.spentOn }.thenByDescending { it.id })

    fun totalPaise(expenses: List<Expense>): Long = expenses.sumOf { it.amountPaise }

    /** Spending per category, largest first, with each category's share of the total in whole percent. */
    fun breakdown(expenses: List<Expense>): List<CategoryTotal> {
        val total = totalPaise(expenses)
        if (total == 0L) return emptyList()
        return expenses.groupBy { it.category }
            .map { (category, items) ->
                val sum = totalPaise(items)
                CategoryTotal(category, sum, roundHalfEven(sum * 100, total))
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.totalPaise }.thenBy { it.category.ordinal })
    }

    fun setBudget(data: ExpenseData, month: YearMonth, amountPaise: Long): ExpenseData =
        if (amountPaise !in 1..Money.MAX_PAISE) data else data.copy(budgets = data.budgets + (month to amountPaise))

    /** Budget minus spending; negative when over budget. Null when no budget is set. */
    fun remainingPaise(budgetPaise: Long?, spentPaise: Long): Long? = budgetPaise?.let { it - spentPaise }

    /** Integer division rounding halves to the even neighbour (what Python's round() does). */
    internal fun roundHalfEven(numerator: Long, denominator: Long): Int {
        val quotient = numerator / denominator
        val twiceRemainder = (numerator % denominator) * 2
        val rounded = when {
            twiceRemainder > denominator -> quotient + 1
            twiceRemainder < denominator -> quotient
            else -> if (quotient % 2 == 0L) quotient else quotient + 1
        }
        return rounded.toInt()
    }
}
