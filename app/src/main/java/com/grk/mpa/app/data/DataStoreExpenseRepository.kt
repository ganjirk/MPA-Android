package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.ExpenseCodec
import com.grk.mpa.app.core.ExpenseData
import kotlinx.coroutines.flow.first

private val Context.expenseDataStore by preferencesDataStore(name = "expenses")

/** Stores expenses on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreExpenseRepository(context: Context) : ExpenseRepository {
    private val dataStore = context.applicationContext.expenseDataStore
    private val key = stringPreferencesKey("expenses_v1")

    override suspend fun load(): ExpenseData =
        ExpenseCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(data: ExpenseData) {
        dataStore.edit { it[key] = ExpenseCodec.encode(data) }
    }
}
