package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.DecisionMakerCodec
import com.grk.mpa.app.core.DecisionMakerData
import kotlinx.coroutines.flow.first

private val Context.decisionMakerStore by preferencesDataStore(
    name = "decision_maker"
)

class DataStoreDecisionMakerRepository(
    context: Context,
    storeOverride: DataStore<Preferences>? = null
) : DecisionMakerRepository {

    private val store = storeOverride
        ?: context.applicationContext.decisionMakerStore

    private val dataKey = stringPreferencesKey("decision_maker_v1")

    override suspend fun load(): DecisionMakerData {
        val preferences = store.data.first()
        val raw = preferences[dataKey].orEmpty()
        return DecisionMakerCodec.decode(raw)
    }

    override suspend fun save(data: DecisionMakerData) {
        store.edit { preferences ->
            preferences[dataKey] = DecisionMakerCodec.encode(data)
        }
    }
}
