package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskCodec
import kotlinx.coroutines.flow.first

private val Context.taskDataStore by preferencesDataStore(name = "tasks")

/**
 * Stores tasks and sync metadata on-device.
 *
 * The existing tasks_v1 key is deliberately retained. TaskCodec reads its
 * original four-column records, so app upgrades preserve all existing tasks.
 */
class DataStoreTaskRepository(context: Context) : SyncAwareTaskRepository {
    private val dataStore = context.applicationContext.taskDataStore
    private val tasksKey = stringPreferencesKey("tasks_v1")
    private val linkedAccountKey = longPreferencesKey("task_sync_account_v1")
    private val pendingDeletesKey = stringPreferencesKey("task_sync_deletes_v1")

    override suspend fun load(): List<Task> = loadSnapshot().tasks

    override suspend fun save(tasks: List<Task>) {
        dataStore.edit { it[tasksKey] = TaskCodec.encode(tasks) }
    }

    override suspend fun loadSnapshot(): TaskStoreSnapshot {
        val preferences = dataStore.data.first()
        return TaskStoreSnapshot(
            tasks = TaskCodec.decode(preferences[tasksKey].orEmpty()),
            linkedAccountId = preferences[linkedAccountKey],
            pendingDeleteRemoteIds = decodeIds(preferences[pendingDeletesKey].orEmpty())
        )
    }

    /** Writes the task list and its account/tombstone metadata in one DataStore transaction. */
    override suspend fun saveSnapshot(snapshot: TaskStoreSnapshot) {
        dataStore.edit { preferences ->
            preferences[tasksKey] = TaskCodec.encode(snapshot.tasks)
            if (snapshot.linkedAccountId == null) {
                preferences.remove(linkedAccountKey)
            } else {
                preferences[linkedAccountKey] = snapshot.linkedAccountId
            }
            if (snapshot.pendingDeleteRemoteIds.isEmpty()) {
                preferences.remove(pendingDeletesKey)
            } else {
                preferences[pendingDeletesKey] = snapshot.pendingDeleteRemoteIds
                    .sorted()
                    .joinToString(",")
            }
        }
    }

    private fun decodeIds(encoded: String): Set<Long> =
        encoded.split(',')
            .mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0L } }
            .toSet()
}
