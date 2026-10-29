package com.grk.mpa.app.data

import com.grk.mpa.app.core.Task

/** Where tasks are stored. The UI only depends on this interface. */
interface TaskRepository {
    suspend fun load(): List<Task>
    suspend fun save(tasks: List<Task>)
}
