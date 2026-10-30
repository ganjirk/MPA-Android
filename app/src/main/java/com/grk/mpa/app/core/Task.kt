package com.grk.mpa.app.core

data class Task(
    /** Stable device-side ID used by the UI and local storage. */
    val id: Long,
    val title: String,
    val isDone: Boolean = false,
    val createdAtMillis: Long = 0L,
    /** Server-side task ID. Null means this task has never been linked to the account. */
    val remoteId: Long? = null,
    val syncState: TaskSyncState = TaskSyncState.LOCAL_ONLY
)

enum class TaskSyncState {
    /** Created on this device; never upload automatically without explicit user consent. */
    LOCAL_ONLY,
    /** Matches the server copy for the currently linked account. */
    SYNCED,
    /** A local edit must be sent to the server. */
    PENDING_UPDATE
}

enum class TaskFilter { ALL, ACTIVE, DONE }
