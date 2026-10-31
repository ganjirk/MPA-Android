package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.LudoCodec
import com.grk.mpa.app.core.LudoState
import kotlinx.coroutines.flow.first

private val Context.ludoDataStore by preferencesDataStore(name = "ludo")

/** Keeps the Ludo game on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreLudoRepository(context: Context) : LudoRepository {
    private val dataStore = context.applicationContext.ludoDataStore
    private val key = stringPreferencesKey("ludo_v1")

    override suspend fun load(): LudoState? = dataStore.data.first()[key]?.let(LudoCodec::decode)

    override suspend fun save(state: LudoState) {
        dataStore.edit { it[key] = LudoCodec.encode(state) }
    }
}
