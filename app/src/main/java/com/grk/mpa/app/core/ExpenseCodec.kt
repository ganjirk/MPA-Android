package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth

/**
 * Plain-text DataStore format. Original six-column E records remain supported.
 * New records: E id paise category yyyy-MM-dd encoded-description remoteId syncState.
 * B records store budgets, A stores the linked account, and D stores pending remote deletes.
 */
object ExpenseCodec {
    private const val TAB = "\t"
    private const val CHARSET = "UTF-8"

    fun encode(data: ExpenseData): String {
        val expenseLines = data.expenses.map { e ->
            val base = listOf("E", e.id, e.amountPaise, e.category.key, e.spentOn,
                URLEncoder.encode(e.description, CHARSET))
            if (e.remoteId == null && e.syncState == ExpenseSyncState.LOCAL_ONLY) {
                base.joinToString(TAB)
            } else {
                (base + listOf(e.remoteId ?: "-", e.syncState.name)).joinToString(TAB)
            }
        }
        val budgetLines = data.budgets.toSortedMap().map { (month, paise) ->
            listOf("B", month, paise).joinToString(TAB)
        }
        val accountLine = data.linkedAccountId?.let { listOf("A", it).joinToString(TAB) }
        val deleteLines = data.pendingDeleteRemoteIds.filter { it > 0L }.sorted()
            .map { listOf("D", it).joinToString(TAB) }
        return (expenseLines + budgetLines + listOfNotNull(accountLine) + deleteLines).joinToString("\n")
    }

    fun decode(text: String): ExpenseData {
        val expenses = mutableListOf<Expense>()
        val budgets = mutableMapOf<YearMonth, Long>()
        var linkedAccountId: Long? = null
        val pendingDeletes = mutableSetOf<Long>()
        for (line in text.lineSequence()) {
            val parts = line.split(TAB)
            when (parts.firstOrNull()) {
                "E" -> parseExpense(parts)?.let(expenses::add)
                "B" -> parseBudget(parts)?.let { budgets[it.first] = it.second }
                "A" -> if (parts.size == 2) linkedAccountId = parts[1].toLongOrNull()?.takeIf { it > 0L }
                "D" -> if (parts.size == 2) parts[1].toLongOrNull()?.takeIf { it > 0L }?.let(pendingDeletes::add)
            }
        }
        return ExpenseData(expenses, budgets, linkedAccountId, pendingDeletes)
    }

    private fun parseExpense(parts: List<String>): Expense? {
        if (parts.size != 6 && parts.size != 8) return null
        val id = parts[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
        val paise = parts[2].toLongOrNull()?.takeIf { it in 1..Money.MAX_PAISE } ?: return null
        val category = ExpenseCategory.fromKey(parts[3]) ?: ExpenseCategory.OTHER
        val date = runCatching { LocalDate.parse(parts[4]) }.getOrNull() ?: return null
        val description = runCatching { URLDecoder.decode(parts[5], CHARSET) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        if (parts.size == 6) return Expense(id, paise, category, description, date)
        val remoteId = parts[6].takeIf { it != "-" }?.toLongOrNull()?.takeIf { it > 0L }
        val state = runCatching { ExpenseSyncState.valueOf(parts[7]) }.getOrNull() ?: return null
        if (state in setOf(ExpenseSyncState.SYNCED, ExpenseSyncState.PENDING_UPDATE) && remoteId == null) return null
        return Expense(id, paise, category, description, date, remoteId, state)
    }

    private fun parseBudget(parts: List<String>): Pair<YearMonth, Long>? {
        if (parts.size != 3) return null
        val month = runCatching { YearMonth.parse(parts[1]) }.getOrNull() ?: return null
        val paise = parts[2].toLongOrNull()?.takeIf { it in 1..Money.MAX_PAISE } ?: return null
        return month to paise
    }
}
