package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.SudokuCodec
import com.grk.mpa.app.core.SudokuStore
import kotlinx.coroutines.flow.first

/** Where Sudoku progress and best times are kept. The UI only depends on this interface. */
interface SudokuRepository {
    suspend fun load(): SudokuStore
    suspend fun save(store: SudokuStore)
}

private val Context.sudokuDataStore by preferencesDataStore(name = "sudoku")

/** Keeps Sudoku progress on the device with DataStore. Nothing is sent over the network. */
class DataStoreSudokuRepository(context: Context) : SudokuRepository {
    private val dataStore = context.applicationContext.sudokuDataStore
    private val key = stringPreferencesKey("sudoku_v1")

    override suspend fun load(): SudokuStore = SudokuCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(store: SudokuStore) {
        dataStore.edit { it[key] = SudokuCodec.encode(store) }
    }
}
