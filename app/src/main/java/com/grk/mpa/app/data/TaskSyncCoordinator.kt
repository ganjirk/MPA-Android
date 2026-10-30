package com.grk.mpa.app.data

import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskSyncState
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.MpaApi
import com.grk.mpa.app.data.api.RemoteTask
import com.grk.mpa.app.data.api.UpdateTaskRequest
import com.grk.mpa.app.data.auth.AuthStorage
import com.grk.mpa.app.data.auth.StoredTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

sealed interface TaskSyncResult {
    data class Success(val imported: Int, val updated: Int, val deleted: Int) : TaskSyncResult
    data object SignedOut : TaskSyncResult
    data object Offline : TaskSyncResult
    data object Failed : TaskSyncResult
    data class AccountMismatch(val linkedAccountId: Long, val signedInAccountId: Long) : TaskSyncResult
}

/**
 * Explicit, conservative synchronization for tasks already linked to the signed-in account.
 *
 * Device-only tasks are intentionally never uploaded here. The first sync imports server tasks
 * and links the store to the account. Switching accounts is blocked rather than reassigning
 * existing server IDs. A separate, deliberate migration flow can be added later.
 */
class TaskSyncCoordinator(
    private val api: MpaApi,
    private val authStorage: AuthStorage,
    private val repository: SyncAwareTaskRepository
) {
    suspend fun sync(): TaskSyncResult {
        val session = authenticatedAccount() ?: return TaskSyncResult.SignedOut
        val (tokens, accountId) = session
        var snapshot = repository.loadSnapshot()
        val linkedAccountId = snapshot.linkedAccountId
        if (linkedAccountId != null && linkedAccountId != accountId) {
            return TaskSyncResult.AccountMismatch(linkedAccountId, accountId)
        }

        val remoteTasks = when (val result = api.tasks(tokens.access)) {
            is ApiResult.Success -> result.value
            ApiResult.NetworkError -> return TaskSyncResult.Offline
            else -> return TaskSyncResult.Failed
        }

        // Initial link: import remote records but never upload pre-existing local-only tasks.
        if (linkedAccountId == null) {
            val imported = remoteTasks.filter { remote ->
                snapshot.tasks.none { it.remoteId == remote.id }
            }.mapIndexed { index, remote ->
                remote.toLocalTask(nextLocalId(snapshot.tasks) + index)
            }
            snapshot = snapshot.copy(
                tasks = snapshot.tasks + imported,
                linkedAccountId = accountId
            )
            repository.saveSnapshot(snapshot)
            return TaskSyncResult.Success(imported = imported.size, updated = 0, deleted = 0)
        }

        var updatedCount = 0
        var deletedCount = 0

        // Tombstones are retained until the server confirms deletion.
        for (remoteId in snapshot.pendingDeleteRemoteIds.toList()) {
            when (val result = api.deleteTask(tokens.access, remoteId)) {
                is ApiResult.Success, ApiResult.Unauthorized -> {
                    // Unauthorized is not confirmation; don't clear a tombstone.
                    if (result == ApiResult.Unauthorized) return TaskSyncResult.Failed
                    snapshot = snapshot.copy(pendingDeleteRemoteIds = snapshot.pendingDeleteRemoteIds - remoteId)
                    repository.saveSnapshot(snapshot)
                    deletedCount++
                }
                ApiResult.NetworkError -> return TaskSyncResult.Offline
                else -> return TaskSyncResult.Failed
            }
        }

        val remoteById = remoteTasks.associateBy { it.id }
        val tasksAfterUpdates = snapshot.tasks.map { local ->
            val remoteId = local.remoteId ?: return@map local
            val remote = remoteById[remoteId] ?: return@map local
            if (local.syncState == TaskSyncState.PENDING_UPDATE) {
                when (val result = api.updateTask(
                    tokens.access,
                    remoteId,
                    UpdateTaskRequest(text = local.title, completed = local.isDone)
                )) {
                    is ApiResult.Success -> {
                        updatedCount++
                        result.value.toLocalTask(local.id)
                    }
                    ApiResult.NetworkError -> return TaskSyncResult.Offline
                    else -> return TaskSyncResult.Failed
                }
            } else {
                remote.toLocalTask(local.id)
            }
        }

        val imported = remoteTasks.filter { remote ->
            tasksAfterUpdates.none { it.remoteId == remote.id } &&
                remote.id !in snapshot.pendingDeleteRemoteIds
        }.mapIndexed { index, remote ->
            remote.toLocalTask(nextLocalId(tasksAfterUpdates) + index)
        }
        snapshot = snapshot.copy(tasks = tasksAfterUpdates + imported)
        repository.saveSnapshot(snapshot)
        return TaskSyncResult.Success(imported = imported.size, updated = updatedCount, deleted = deletedCount)
    }

    private suspend fun authenticatedAccount(): Pair<StoredTokens, Long>? {
        var tokens = authStorage.load() ?: return null
        when (val result = api.account(tokens.access)) {
            is ApiResult.Success -> return tokens to result.value.id
            ApiResult.Unauthorized -> {
                when (val refreshed = api.refresh(tokens.refresh)) {
                    is ApiResult.Success -> {
                        tokens = StoredTokens(
                            refreshed.value.access,
                            refreshed.value.refresh ?: tokens.refresh
                        )
                        authStorage.save(tokens)
                        return when (val account = api.account(tokens.access)) {
                            is ApiResult.Success -> tokens to account.value.id
                            ApiResult.Unauthorized -> {
                                authStorage.clear()
                                null
                            }
                            else -> null
                        }
                    }
                    ApiResult.Unauthorized -> {
                        authStorage.clear()
                        return null
                    }
                    else -> return null
                }
            }
            else -> return null
        }
    }

    private fun nextLocalId(tasks: List<Task>): Long = (tasks.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun RemoteTask.toLocalTask(localId: Long): Task {
        val createdMillis = runCatching { Instant.parse(createdDate).toEpochMilli() }
            .getOrElse { runCatching { LocalDate.parse(createdDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrDefault(0L) }
        return Task(
            id = localId,
            title = text,
            isDone = completed,
            createdAtMillis = createdMillis,
            remoteId = id,
            syncState = TaskSyncState.SYNCED
        )
    }
}
