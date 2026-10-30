package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.Reminder
import com.grk.mpa.app.core.ReminderCodec
import kotlinx.coroutines.flow.first

private val Context.reminderStore by preferencesDataStore(name = "reminders")

class DataStoreReminderRepository(context: Context) : ReminderRepository {
    private val store = context.applicationContext.reminderStore
    private val key = stringPreferencesKey("reminders_v1")
    override suspend fun load(): List<Reminder> = ReminderCodec.decode(store.data.first()[key].orEmpty())
    override suspend fun save(items: List<Reminder>) {
        store.edit { it[key] = ReminderCodec.encode(items) }
    }
}
