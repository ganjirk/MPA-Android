package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.TicTacToeCodec
import com.grk.mpa.app.core.TicTacToeStats
import kotlinx.coroutines.flow.first

private val Context.ticTacToeDataStore by preferencesDataStore(name = "tictactoe")

/** Stores Tic Tac Toe results on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreTicTacToeRepository(context: Context) : TicTacToeRepository {
    private val dataStore = context.applicationContext.ticTacToeDataStore
    private val key = stringPreferencesKey("tictactoe_v1")

    override suspend fun load(): TicTacToeStats =
        TicTacToeCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(stats: TicTacToeStats) {
        dataStore.edit { it[key] = TicTacToeCodec.encode(stats) }
    }
}
