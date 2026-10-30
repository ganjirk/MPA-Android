package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.GuessRecord
import com.grk.mpa.app.core.GuessRecordCodec
import com.grk.mpa.app.core.GuessStats
import kotlinx.coroutines.flow.first

/** Where finished Number Guess games are kept. */
interface GuessStatsRepository {
    suspend fun load(): List<GuessRecord>
    suspend fun append(record: GuessRecord)
}

private val Context.guessStatsDataStore by preferencesDataStore(name = "guess_stats")

/** Keeps the records on the device with DataStore. Nothing is sent over the network. */
class DataStoreGuessStatsRepository(context: Context) : GuessStatsRepository {
    private val dataStore = context.applicationContext.guessStatsDataStore
    private val key = stringPreferencesKey("records_v1")

    override suspend fun load(): List<GuessRecord> =
        GuessRecordCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun append(record: GuessRecord) {
        dataStore.edit { prefs ->
            val updated = GuessStats.append(GuessRecordCodec.decode(prefs[key].orEmpty()), record)
            prefs[key] = GuessRecordCodec.encode(updated)
        }
    }
}
