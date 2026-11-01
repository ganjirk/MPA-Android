package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.ChessCodec
import com.grk.mpa.app.core.ChessSave
import kotlinx.coroutines.flow.first

private val Context.chessDataStore by preferencesDataStore(name = "chess")

/** Keeps the Chess game on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreChessRepository(context: Context) : ChessRepository {
    private val dataStore = context.applicationContext.chessDataStore
    private val key = stringPreferencesKey("chess_v1")

    override suspend fun load(): ChessSave? = dataStore.data.first()[key]?.let(ChessCodec::decode)

    override suspend fun save(save: ChessSave) {
        dataStore.edit { it[key] = ChessCodec.encode(save) }
    }
}
