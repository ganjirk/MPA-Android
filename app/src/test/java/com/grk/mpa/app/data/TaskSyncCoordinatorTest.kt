package com.grk.mpa.app.data

import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskSyncState
import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.CreateTaskRequest
import com.grk.mpa.app.data.api.MpaApi
import com.grk.mpa.app.data.api.RefreshedTokens
import com.grk.mpa.app.data.api.RemoteTask
import com.grk.mpa.app.data.api.TokenPair
import com.grk.mpa.app.data.api.UpdateTaskRequest
import com.grk.mpa.app.data.auth.AuthStorage
import com.grk.mpa.app.data.auth.StoredTokens
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class SyncTestAuthStorage(var tokens: StoredTokens? = StoredTokens("access", "refresh")) : AuthStorage {
    override suspend fun load(): StoredTokens? = tokens
    override suspend fun save(tokens: StoredTokens) { this.tokens = tokens }
    override suspend fun clear() { tokens = null }
}

private class SyncTestTaskRepository(var snapshot: TaskStoreSnapshot) : SyncAwareTaskRepository {
    override suspend fun load(): List<Task> = snapshot.tasks
    override suspend fun save(tasks: List<Task>) { snapshot = snapshot.copy(tasks = tasks) }
    override suspend fun loadSnapshot(): TaskStoreSnapshot = snapshot
    override suspend fun saveSnapshot(snapshot: TaskStoreSnapshot) { this.snapshot = snapshot }
}

private class SyncTestApi : MpaApi {
    var accountResult: ApiResult<Account> = ApiResult.Success(Account(5, "user"))
    var refreshResult: ApiResult<RefreshedTokens> = ApiResult.Success(RefreshedTokens("new-access"))
    var tasksResult: ApiResult<List<RemoteTask>> = ApiResult.Success(emptyList())
    val updateCalls = mutableListOf<Pair<Long, UpdateTaskRequest>>()
    val deleteCalls = mutableListOf<Long>()
    var createCalls = 0
    var accountCalls = 0
    var taskCalls = 0

    override suspend fun login(username: String, password: String): ApiResult<TokenPair> =
        ApiResult.HttpError(501)
    override suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens> = refreshResult
    override suspend fun account(accessToken: String): ApiResult<Account> {
        accountCalls++
        return if (accessToken == "new-access") ApiResult.Success(Account(5, "user")) else accountResult
    }
    override suspend fun tasks(accessToken: String): ApiResult<List<RemoteTask>> {
        taskCalls++
        return tasksResult
    }
    override suspend fun createTask(accessToken: String, request: CreateTaskRequest): ApiResult<RemoteTask> {
        createCalls++
        return ApiResult.HttpError(501)
    }
    override suspend fun updateTask(
        accessToken: String,
        id: Long,
        request: UpdateTaskRequest
    ): ApiResult<RemoteTask> {
        updateCalls += id to request
        val source = (tasksResult as ApiResult.Success).value.first { it.id == id }
        return ApiResult.Success(source.copy(text = request.text ?: source.text, completed = request.completed ?: source.completed))
    }
    override suspend fun deleteTask(accessToken: String, id: Long): ApiResult<Unit> {
        deleteCalls += id
        return ApiResult.Success(Unit)
    }
}

private fun remoteTask(id: Long, text: String, completed: Boolean = false) = RemoteTask(
    id = id,
    text = text,
    completed = completed,
    createdDate = "2026-10-30T10:00:00Z"
)

class TaskSyncCoordinatorTest {
    @Test
    fun firstSyncImportsServerTasksButNeverUploadsExistingLocalTasks() = runBlocking {
        val local = Task(1, "My offline task")
        val repo = SyncTestTaskRepository(TaskStoreSnapshot(listOf(local)))
        val api = SyncTestApi().apply { tasksResult = ApiResult.Success(listOf(remoteTask(90, "Website task"))) }
        val result = TaskSyncCoordinator(api, SyncTestAuthStorage(), repo).sync()

        assertEquals(TaskSyncResult.Success(imported = 1, updated = 0, deleted = 0), result)
        assertEquals(5L, repo.snapshot.linkedAccountId)
        assertEquals(listOf("My offline task", "Website task"), repo.snapshot.tasks.map { it.title })
        assertEquals(null, repo.snapshot.tasks.first().remoteId)
        assertEquals(90L, repo.snapshot.tasks.last().remoteId)
        assertEquals(TaskSyncState.SYNCED, repo.snapshot.tasks.last().syncState)
        assertEquals(0, api.createCalls)
    }

    @Test
    fun anotherSignedInAccountCannotAdoptExistingRemoteIds() = runBlocking {
        val linkedTask = Task(1, "Private task", remoteId = 90, syncState = TaskSyncState.SYNCED)
        val repo = SyncTestTaskRepository(TaskStoreSnapshot(listOf(linkedTask), linkedAccountId = 5))
        val api = SyncTestApi().apply { accountResult = ApiResult.Success(Account(6, "other")) }

        val result = TaskSyncCoordinator(api, SyncTestAuthStorage(), repo).sync()

        assertEquals(TaskSyncResult.AccountMismatch(5, 6), result)
        assertEquals(0, api.taskCalls)
        assertEquals(linkedTask, repo.snapshot.tasks.single())
        assertEquals(5L, repo.snapshot.linkedAccountId)
    }

    @Test
    fun linkedSyncUpdatesMappedTasksDeletesTombstonesAndImportsNewRemoteTasks() = runBlocking {
        val pending = Task(
            id = 1,
            title = "Edited locally",
            isDone = true,
            remoteId = 90,
            syncState = TaskSyncState.PENDING_UPDATE
        )
        val repo = SyncTestTaskRepository(
            TaskStoreSnapshot(
                tasks = listOf(pending),
                linkedAccountId = 5,
                pendingDeleteRemoteIds = setOf(91)
            )
        )
        val api = SyncTestApi().apply {
            tasksResult = ApiResult.Success(
                listOf(remoteTask(90, "Old server title"), remoteTask(91, "Deleted task"), remoteTask(92, "New task"))
            )
        }

        val result = TaskSyncCoordinator(api, SyncTestAuthStorage(), repo).sync()

        assertEquals(TaskSyncResult.Success(imported = 1, updated = 1, deleted = 1), result)
        assertEquals(listOf(91L), api.deleteCalls)
        assertEquals(90L, api.updateCalls.single().first)
        assertEquals("Edited locally", api.updateCalls.single().second.text)
        assertEquals(true, api.updateCalls.single().second.completed)
        assertEquals(listOf("Edited locally", "New task"), repo.snapshot.tasks.map { it.title })
        assertEquals(setOf(90L, 92L), repo.snapshot.tasks.mapNotNull { it.remoteId }.toSet())
        assertTrue(repo.snapshot.pendingDeleteRemoteIds.isEmpty())
    }

    @Test
    fun networkFailureDoesNotClearLinkedAccountOrDeletionTombstones() = runBlocking {
        val repo = SyncTestTaskRepository(
            TaskStoreSnapshot(emptyList(), linkedAccountId = 5, pendingDeleteRemoteIds = setOf(91))
        )
        val api = SyncTestApi().apply { tasksResult = ApiResult.NetworkError }

        assertEquals(TaskSyncResult.Offline, TaskSyncCoordinator(api, SyncTestAuthStorage(), repo).sync())
        assertEquals(5L, repo.snapshot.linkedAccountId)
        assertEquals(setOf(91L), repo.snapshot.pendingDeleteRemoteIds)
    }

    @Test
    fun expiredAccessTokenIsRefreshedBeforeSync() = runBlocking {
        val auth = SyncTestAuthStorage(StoredTokens("old-access", "refresh"))
        val api = SyncTestApi().apply {
            accountResult = ApiResult.Unauthorized
            refreshResult = ApiResult.Success(RefreshedTokens("new-access", "new-refresh"))
        }
        val repo = SyncTestTaskRepository(TaskStoreSnapshot(emptyList()))
        val result = TaskSyncCoordinator(api, auth, repo).sync()

        assertTrue(result is TaskSyncResult.Success)
        assertEquals(StoredTokens("new-access", "new-refresh"), auth.tokens)
        assertEquals(2, api.accountCalls)
    }
}
