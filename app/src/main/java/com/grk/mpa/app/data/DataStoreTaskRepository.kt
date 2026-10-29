package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskCodec
import kotlinx.coroutines.flow.first

private val Context.taskDataStore by preferencesDataStore(name = "tasks")

/** Stores tasks on the device with Jetpack DataStore. Nothing is sent over the network. */
class DataStoreTaskRepository(context: Context) : TaskRepository {
    private val dataStore = context.applicationContext.taskDataStore
    private val key = stringPreferencesKey("tasks_v1")

    override suspend fun load(): List<Task> =
        TaskCodec.decode(dataStore.data.first()[key].orEmpty())

    override suspend fun save(tasks: List<Task>) {
        dataStore.edit { it[key] = TaskCodec.encode(tasks) }
    }
}
