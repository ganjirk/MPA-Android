package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.ExpenseCodec
import com.grk.mpa.app.core.ExpenseData
import kotlinx.coroutines.flow.first

private val Context.expenseDataStore by preferencesDataStore(name = "expenses")

/** Stores expenses and sync metadata locally. Network sync is handled by a separate layer. */
class DataStoreExpenseRepository(context: Context) : SyncAwareExpenseRepository {
    private val dataStore = context.applicationContext.expenseDataStore
    // Keep the existing file and key: old users' data is decoded in-place, not reset.
    private val key = stringPreferencesKey("expenses_v1")

    override suspend fun load(): ExpenseData =
        ExpenseCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(data: ExpenseData) {
        dataStore.edit { it[key] = ExpenseCodec.encode(data) }
    }

    override suspend fun loadSnapshot(): ExpenseStoreSnapshot {
        val data = load()
        return ExpenseStoreSnapshot(
            expenses = data.expenses,
            budgets = data.budgets,
            linkedAccountId = data.linkedAccountId,
            pendingDeleteRemoteIds = data.pendingDeleteRemoteIds
        )
    }

    /** Data and sync metadata are written in one DataStore transaction. */
    override suspend fun saveSnapshot(snapshot: ExpenseStoreSnapshot) {
        dataStore.edit {
            it[key] = ExpenseCodec.encode(
                ExpenseData(
                    expenses = snapshot.expenses,
                    budgets = snapshot.budgets,
                    linkedAccountId = snapshot.linkedAccountId,
                    pendingDeleteRemoteIds = snapshot.pendingDeleteRemoteIds
                )
            )
        }
    }
}
