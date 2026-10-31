package com.grk.mpa.app.data

import com.grk.mpa.app.core.Task

/** Where tasks are stored. The UI only depends on this interface. */
interface TaskRepository {
    suspend fun load(): List<Task>
    suspend fun save(tasks: List<Task>)
}

/**
 * Optional extension for repositories that persist sync metadata atomically
 * with the task list. Existing TaskRepository implementations remain valid.
 */
interface SyncAwareTaskRepository : TaskRepository {
    suspend fun loadSnapshot(): TaskStoreSnapshot
    suspend fun saveSnapshot(snapshot: TaskStoreSnapshot)
}
