package com.grk.mpa.app.data

import com.grk.mpa.app.core.Expense
import com.grk.mpa.app.core.ExpenseData
import java.time.YearMonth

/** Where expenses and budgets are stored. The UI only depends on this interface. */
interface ExpenseRepository {
    suspend fun load(): ExpenseData
    suspend fun save(data: ExpenseData)
}

/** Optional extension for sync-aware repositories; plain ExpenseRepository implementations still work. */
interface SyncAwareExpenseRepository : ExpenseRepository {
    suspend fun loadSnapshot(): ExpenseStoreSnapshot
    suspend fun saveSnapshot(snapshot: ExpenseStoreSnapshot)
}

data class ExpenseStoreSnapshot(
    val expenses: List<Expense> = emptyList(),
    val budgets: Map<YearMonth, Long> = emptyMap(),
    val linkedAccountId: Long? = null,
    val pendingDeleteRemoteIds: Set<Long> = emptySet()
) {
    init {
        require(linkedAccountId == null || linkedAccountId > 0L)
        require(pendingDeleteRemoteIds.all { it > 0L })
        require(expenses.all { it.remoteId == null || it.remoteId > 0L })
    }
}
