package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.MatchHistoryCodec
import com.grk.mpa.app.core.MatchResult
import kotlinx.coroutines.flow.first

/** Where finished Match Cards games are kept. The UI only depends on this interface. */
interface MatchCardsRepository {
    suspend fun load(): List<MatchResult>
    suspend fun save(results: List<MatchResult>)
}

private val Context.matchCardsDataStore by preferencesDataStore(name = "match_cards")

/** Keeps results on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreMatchCardsRepository(context: Context) : MatchCardsRepository {
    private val dataStore = context.applicationContext.matchCardsDataStore
    private val key = stringPreferencesKey("match_cards_v1")

    override suspend fun load(): List<MatchResult> = MatchHistoryCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(results: List<MatchResult>) {
        dataStore.edit { it[key] = MatchHistoryCodec.encode(results) }
    }
}
