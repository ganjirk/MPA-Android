package com.grk.mpa.app.core

import java.time.LocalDate
import java.time.YearMonth

/** The same eight categories as the website's Expense Tracker. */
enum class ExpenseCategory(val key: String, val label: String) {
    FOOD("food", "Food & dining"),
    TRANSPORT("transport", "Transport"),
    HOME("home", "Home & bills"),
    SHOPPING("shopping", "Shopping"),
    HEALTH("health", "Health"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    EDUCATION("education", "Education"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String): ExpenseCategory? = entries.firstOrNull { it.key == key }
    }
}

data class Expense(
    val id: Long,
    val amountPaise: Long,
    val category: ExpenseCategory,
    val description: String,
    val spentOn: LocalDate
)

/** Everything the tracker stores: the expenses and one optional budget (in paise) per month. */
data class ExpenseData(
    val expenses: List<Expense> = emptyList(),
    val budgets: Map<YearMonth, Long> = emptyMap()
)

data class CategoryTotal(val category: ExpenseCategory, val totalPaise: Long, val percent: Int)
