package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.ContactHistoryCodec
import com.grk.mpa.app.core.SentMessage
import kotlinx.coroutines.flow.first

/** Where the messages sent from this device are listed. The UI only depends on this interface. */
interface ContactHistoryRepository {
    suspend fun load(): List<SentMessage>
    suspend fun save(messages: List<SentMessage>)
}

private val Context.contactHistoryDataStore by preferencesDataStore(name = "contact_history")

/** Keeps the list on the device with Jetpack DataStore. Nothing extra is sent over the network. */
class DataStoreContactHistoryRepository(context: Context) : ContactHistoryRepository {
    private val dataStore = context.applicationContext.contactHistoryDataStore
    private val key = stringPreferencesKey("contact_history_v1")

    override suspend fun load(): List<SentMessage> =
        ContactHistoryCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(messages: List<SentMessage>) {
        dataStore.edit { it[key] = ContactHistoryCodec.encode(messages) }
    }
}
