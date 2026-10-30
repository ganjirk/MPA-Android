package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth

/**
 * Stores [ExpenseData] as plain text, one record per line, fields separated by tabs:
 * - `E id paise categoryKey yyyy-MM-dd url-encoded-description`
 * - `B yyyy-MM paise`
 * Descriptions are URL-encoded so tabs and line breaks cannot break the format.
 * Lines that cannot be read are skipped instead of failing the whole file.
 */
object ExpenseCodec {
    private const val TAB = "\t"
    private const val CHARSET = "UTF-8"

    fun encode(data: ExpenseData): String {
        val lines = data.expenses.map { e ->
            listOf(
                "E", e.id, e.amountPaise, e.category.key, e.spentOn,
                URLEncoder.encode(e.description, CHARSET)
            ).joinToString(TAB)
        } + data.budgets.toSortedMap().map { (month, paise) -> listOf("B", month, paise).joinToString(TAB) }
        return lines.joinToString("\n")
    }

    fun decode(text: String): ExpenseData {
        val expenses = mutableListOf<Expense>()
        val budgets = mutableMapOf<YearMonth, Long>()
        for (line in text.lineSequence()) {
            val parts = line.split(TAB)
            when (parts.firstOrNull()) {
                "E" -> parseExpense(parts)?.let(expenses::add)
                "B" -> parseBudget(parts)?.let { budgets[it.first] = it.second }
            }
        }
        return ExpenseData(expenses, budgets)
    }

    private fun parseExpense(parts: List<String>): Expense? {
        if (parts.size != 6) return null
        val id = parts[1].toLongOrNull() ?: return null
        val paise = parts[2].toLongOrNull()?.takeIf { it in 1..Money.MAX_PAISE } ?: return null
        val category = ExpenseCategory.fromKey(parts[3]) ?: ExpenseCategory.OTHER
        val date = runCatching { LocalDate.parse(parts[4]) }.getOrNull() ?: return null
        val description = runCatching { URLDecoder.decode(parts[5], CHARSET) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        return Expense(id, paise, category, description, date)
    }

    private fun parseBudget(parts: List<String>): Pair<YearMonth, Long>? {
        if (parts.size != 3) return null
        val month = runCatching { YearMonth.parse(parts[1]) }.getOrNull() ?: return null
        val paise = parts[2].toLongOrNull()?.takeIf { it in 1..Money.MAX_PAISE } ?: return null
        return month to paise
    }
}
