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

enum class ExpenseSyncState { LOCAL_ONLY, PENDING_CREATE, SYNCED, PENDING_UPDATE }

data class Expense(
    /** Stable device-side ID used by the UI and local storage. */
    val id: Long,
    val amountPaise: Long,
    val category: ExpenseCategory,
    val description: String,
    val spentOn: LocalDate,
    /** Server-side ID; deliberately separate from the device-side ID. */
    val remoteId: Long? = null,
    /** Old records default to LOCAL_ONLY and are never automatically uploaded. */
    val syncState: ExpenseSyncState = ExpenseSyncState.LOCAL_ONLY
)

/** Existing local data plus optional sync metadata. */
data class ExpenseData(
    val expenses: List<Expense> = emptyList(),
    val budgets: Map<YearMonth, Long> = emptyMap(),
    val linkedAccountId: Long? = null,
    val pendingDeleteRemoteIds: Set<Long> = emptySet()
)

data class CategoryTotal(val category: ExpenseCategory, val totalPaise: Long, val percent: Int)
