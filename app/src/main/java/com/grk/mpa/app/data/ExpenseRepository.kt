package com.grk.mpa.app.data

import com.grk.mpa.app.core.ExpenseData

/** Where expenses and budgets are stored. The UI only depends on this interface. */
interface ExpenseRepository {
    suspend fun load(): ExpenseData
    suspend fun save(data: ExpenseData)
}
