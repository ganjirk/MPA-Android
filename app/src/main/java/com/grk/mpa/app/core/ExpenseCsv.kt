package com.grk.mpa.app.core

import java.time.YearMonth

/** Builds the same CSV the website exports for a month. */
object ExpenseCsv {
    /** Lets Excel read non-English text correctly. */
    private const val BOM = "﻿"
    private const val EOL = "\r\n"
    private val formulaStarts = setOf('=', '+', '-', '@', '\t', '\r')

    fun fileName(month: YearMonth): String = "expenses-$month.csv"

    /** Stops spreadsheet apps from treating text that starts with = + - @ as a formula. */
    fun csvSafe(text: String): String =
        if (text.isNotEmpty() && text.first() in formulaStarts) "'$text" else text

    private fun field(text: String): String =
        if (text.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"" + text.replace("\"", "\"\"") + "\"" else text

    fun build(month: YearMonth, expenses: List<Expense>): String {
        val rows = ExpenseOperations.inMonth(expenses, month)
            .sortedWith(compareBy<Expense> { it.spentOn }.thenBy { it.id })
        val sb = StringBuilder(BOM)
        sb.append(listOf("Date", "Description", "Category", "Amount (INR)").joinToString(",") { field(it) }).append(EOL)
        for (e in rows) {
            sb.append(
                listOf(e.spentOn.toString(), csvSafe(e.description), e.category.label, Money.formatAmount(e.amountPaise))
                    .joinToString(",") { field(it) }
            ).append(EOL)
        }
        sb.append(EOL)
        sb.append("Total,,,").append(Money.formatAmount(ExpenseOperations.totalPaise(rows))).append(EOL)
        return sb.toString()
    }
}
