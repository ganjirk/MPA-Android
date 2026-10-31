package com.grk.mpa.app.data

import com.grk.mpa.app.core.Task

/**
 * Atomic local snapshot used by the future sync coordinator.
 *
 * pendingDeleteRemoteIds are tombstones: a remote task was deleted locally
 * and must remain queued until the server confirms its deletion.
 */
data class TaskStoreSnapshot(
    val tasks: List<Task>,
    /** Account whose remote IDs are attached to these tasks; null means unlinked. */
    val linkedAccountId: Long? = null,
    val pendingDeleteRemoteIds: Set<Long> = emptySet()
) {
    init {
        require(linkedAccountId == null || linkedAccountId > 0L)
        require(pendingDeleteRemoteIds.all { it > 0L })
    }
}
