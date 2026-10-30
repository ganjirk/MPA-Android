package com.grk.mpa.app.ui.productivity

import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskFilter
import com.grk.mpa.app.core.TaskSyncState
import com.grk.mpa.app.data.TaskRepository
import com.grk.mpa.app.data.SyncAwareTaskRepository
import com.grk.mpa.app.data.TaskStoreSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRepository(var stored: List<Task> = emptyList()) : TaskRepository {
    var saveCount = 0
    override suspend fun load(): List<Task> = stored
    override suspend fun save(tasks: List<Task>) {
        saveCount++
        stored = tasks
    }
}

 
private class FakeSyncRepository(var snapshot: TaskStoreSnapshot) : SyncAwareTaskRepository {
    var saveCount = 0
    override suspend fun load(): List<Task> = snapshot.tasks
    override suspend fun save(tasks: List<Task>) {
        snapshot = snapshot.copy(tasks = tasks)
        saveCount++
    }
    override suspend fun loadSnapshot(): TaskStoreSnapshot = snapshot
    override suspend fun saveSnapshot(snapshot: TaskStoreSnapshot) {
        this.snapshot = snapshot
        saveCount++
    }
}

class TasksViewModelTest {
    private fun viewModel(repo: FakeRepository = FakeRepository(), now: Long = 1000L) =
        TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined)) { now }

    @Test
    fun loadsStoredTasksOnCreation() {
        val repo = FakeRepository(listOf(Task(1, "Stored", false, 1)))
        val vm = viewModel(repo)
        assertTrue(vm.isLoaded)
        assertEquals(listOf("Stored"), vm.tasks.map { it.title })
    }

    @Test
    fun addTaskUpdatesStateClearsInputAndSaves() {
        val repo = FakeRepository()
        val vm = viewModel(repo, now = 42)
        vm.onNewTitleChange("  Buy milk ")
        assertTrue(vm.canAdd)
        vm.addTask()
        assertEquals(listOf(Task(1, "Buy milk", false, 42)), vm.tasks)
        assertEquals("", vm.newTitle)
        assertEquals(vm.tasks, repo.stored)
    }

    @Test
    fun blankTitleCannotBeAdded() {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.onNewTitleChange("   ")
        assertFalse(vm.canAdd)
        vm.addTask()
        assertTrue(vm.tasks.isEmpty())
        assertEquals(0, repo.saveCount)
    }

    @Test
    fun toggleDeleteAndClearCompletedPersist() {
        val repo = FakeRepository(listOf(Task(1, "A"), Task(2, "B"), Task(3, "C")))
        val vm = viewModel(repo)
        vm.toggle(1)
        vm.toggle(2)
        assertEquals(1, vm.activeCount)
        assertTrue(vm.hasCompleted)
        vm.delete(3)
        assertEquals(listOf(1L, 2L), repo.stored.map { it.id })
        vm.clearCompleted()
        assertTrue(vm.tasks.isEmpty())
        assertTrue(repo.stored.isEmpty())
        assertFalse(vm.hasCompleted)
    }

    @Test
    fun filterControlsVisibleTasks() {
        val repo = FakeRepository(listOf(Task(1, "A", false), Task(2, "B", true)))
        val vm = viewModel(repo)
        assertEquals(2, vm.visibleTasks.size)
        vm.onFilterChange(TaskFilter.ACTIVE)
        assertEquals(listOf(1L), vm.visibleTasks.map { it.id })
        vm.onFilterChange(TaskFilter.DONE)
        assertEquals(listOf(2L), vm.visibleTasks.map { it.id })
    }


    @Test
    fun loadsSyncMetadataAlongsideTasks() {
        val initial = TaskStoreSnapshot(
            tasks = listOf(Task(7, "Linked", remoteId = 70, syncState = com.grk.mpa.app.core.TaskSyncState.SYNCED)),
            linkedAccountId = 12,
            pendingDeleteRemoteIds = setOf(71)
        )
        val repo = FakeSyncRepository(initial)
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))
        assertEquals(initial.tasks, vm.tasks)
        assertTrue(vm.isLoaded)
        assertEquals(initial, repo.snapshot)
    }


    @Test
    fun tasksAddedAfterAccountLinkingAreQueuedForUpload() {
        val repo = FakeSyncRepository(TaskStoreSnapshot(emptyList(), linkedAccountId = 12))
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined)) { 200L }
        vm.onNewTitleChange("New remote task")
        vm.addTask()

        assertEquals(1, vm.tasks.size)
        assertEquals(com.grk.mpa.app.core.TaskSyncState.PENDING_CREATE, vm.tasks.single().syncState)
        assertEquals(12L, repo.snapshot.linkedAccountId)
    }

    @Test
    fun deletingLinkedTaskPersistsRemoteDeletionTombstone() {
        val linked = Task(7, "Linked", remoteId = 70, syncState = com.grk.mpa.app.core.TaskSyncState.SYNCED)
        val repo = FakeSyncRepository(TaskStoreSnapshot(listOf(linked), linkedAccountId = 12))
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))
        vm.delete(linked.id)
        assertTrue(vm.tasks.isEmpty())
        assertEquals(setOf(70L), repo.snapshot.pendingDeleteRemoteIds)
        assertEquals(12L, repo.snapshot.linkedAccountId)
    }

    @Test
    fun deletingDeviceOnlyTaskDoesNotCreateRemoteTombstone() {
        val local = Task(8, "Local")
        val repo = FakeSyncRepository(TaskStoreSnapshot(listOf(local)))
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))
        vm.delete(local.id)
        assertTrue(vm.tasks.isEmpty())
        assertTrue(repo.snapshot.pendingDeleteRemoteIds.isEmpty())
    }

    @Test
    fun legacyTasksCannotBePreparedUntilStoreIsLinkedToAnAccount() {
        val repo = FakeSyncRepository(TaskStoreSnapshot(listOf(Task(1, "Offline task"))))
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))

        assertFalse(vm.canMigrateLegacyTasks)
        vm.toggleLegacyTaskSelection(1)
        vm.prepareSelectedLegacyTasksForUpload()

        assertEquals(TaskSyncState.LOCAL_ONLY, vm.tasks.single().syncState)
        assertEquals(emptySet<Long>(), vm.selectedLegacyTaskIds)
    }

    @Test
    fun preparingMigrationQueuesOnlyExplicitlySelectedLocalTasks() {
        val localOne = Task(1, "Old offline task")
        val localTwo = Task(2, "Another offline task")
        val linked = Task(3, "Already synced", remoteId = 30, syncState = TaskSyncState.SYNCED)
        val repo = FakeSyncRepository(
            TaskStoreSnapshot(listOf(localOne, localTwo, linked), linkedAccountId = 12)
        )
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))

        assertTrue(vm.canMigrateLegacyTasks)
        assertEquals(2, vm.localOnlyTaskCount)
        vm.toggleLegacyTaskSelection(localTwo.id)
        vm.prepareSelectedLegacyTasksForUpload()

        assertEquals(TaskSyncState.LOCAL_ONLY, vm.tasks.first { it.id == localOne.id }.syncState)
        assertEquals(TaskSyncState.PENDING_CREATE, vm.tasks.first { it.id == localTwo.id }.syncState)
        assertEquals(linked, vm.tasks.first { it.id == linked.id })
        assertEquals(emptySet<Long>(), vm.selectedLegacyTaskIds)
        assertEquals(TaskSyncState.PENDING_CREATE, repo.snapshot.tasks.first { it.id == localTwo.id }.syncState)
        assertEquals(12L, repo.snapshot.linkedAccountId)
    }

    @Test
    fun legacySelectionIsClearedWhenSelectedTaskIsDeleted() {
        val repo = FakeSyncRepository(
            TaskStoreSnapshot(listOf(Task(1, "Old offline task")), linkedAccountId = 12)
        )
        val vm = TasksViewModel(repo, CoroutineScope(Dispatchers.Unconfined))
        vm.toggleLegacyTaskSelection(1)
        assertEquals(setOf(1L), vm.selectedLegacyTaskIds)

        vm.delete(1)

        assertEquals(emptySet<Long>(), vm.selectedLegacyTaskIds)
        assertTrue(vm.tasks.isEmpty())
    }

    @Test
    fun newTitleIsLimitedInLength() {
        val vm = viewModel()
        vm.onNewTitleChange("x".repeat(500))
        assertEquals(100, vm.newTitle.length)
    }
}
