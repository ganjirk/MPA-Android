package com.grk.mpa.app.ui.productivity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskFilter
import com.grk.mpa.app.core.TaskOperations
import com.grk.mpa.app.core.TaskSyncState
import com.grk.mpa.app.data.TaskRepository
import com.grk.mpa.app.data.SyncAwareTaskRepository
import com.grk.mpa.app.data.TaskStoreSnapshot
import com.grk.mpa.app.data.TaskSyncCoordinator
import com.grk.mpa.app.data.TaskSyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 */
enum class TaskSyncUiStatus { IDLE, SYNCING, SUCCESS, SIGNED_OUT, OFFLINE, ACCOUNT_MISMATCH, FAILED }

class TasksViewModel(
    private val repository: TaskRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val syncCoordinator: TaskSyncCoordinator? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope
    private var linkedAccountId: Long? = null
    private val pendingDeleteRemoteIds = mutableSetOf<Long>()
    private var persistenceJob: Job? = null

    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var newTitle by mutableStateOf("")
        private set
    var filter by mutableStateOf(TaskFilter.ALL)
        private set
    var syncStatus by mutableStateOf(TaskSyncUiStatus.IDLE)
        private set

    val visibleTasks: List<Task> get() = TaskOperations.filter(tasks, filter)
    val activeCount: Int get() = TaskOperations.activeCount(tasks)
    val hasCompleted: Boolean get() = tasks.any { it.isDone }
    val canAdd: Boolean get() = isLoaded && TaskOperations.normalizeTitle(newTitle) != null

    init {
        scope.launch {
            val syncRepository = repository as? SyncAwareTaskRepository
            if (syncRepository != null) {
                val snapshot = syncRepository.loadSnapshot()
                tasks = snapshot.tasks
                linkedAccountId = snapshot.linkedAccountId
                pendingDeleteRemoteIds.addAll(snapshot.pendingDeleteRemoteIds)
            } else {
                tasks = repository.load()
            }
            isLoaded = true
        }
    }

    fun onNewTitleChange(text: String) {
        if (syncStatus != TaskSyncUiStatus.SYNCING) {
            newTitle = text.take(TaskOperations.MAX_TITLE_LENGTH)
        }
    }

    fun onFilterChange(newFilter: TaskFilter) {
        filter = newFilter
    }

    fun addTask() {
        if (!canAdd || syncStatus == TaskSyncUiStatus.SYNCING) return
        val added = TaskOperations.add(tasks, newTitle, clock())
        val prepared = if (linkedAccountId != null && added.size > tasks.size) {
            added.mapIndexed { index, task ->
                if (index == added.lastIndex) task.copy(syncState = TaskSyncState.PENDING_CREATE) else task
            }
        } else added
        update(prepared)
        newTitle = ""
    }

    fun toggle(id: Long) {
        if (syncStatus != TaskSyncUiStatus.SYNCING) update(TaskOperations.toggle(tasks, id))
    }

    fun delete(id: Long) {
        if (syncStatus != TaskSyncUiStatus.SYNCING) update(TaskOperations.delete(tasks, id))
    }

    fun clearCompleted() {
        if (syncStatus != TaskSyncUiStatus.SYNCING) update(TaskOperations.clearCompleted(tasks))
    }

    /** Runs only after an explicit user action; local-only tasks are never uploaded. */
    fun syncTasks() {
        val coordinator = syncCoordinator ?: run {
            syncStatus = TaskSyncUiStatus.FAILED
            return
        }
        if (syncStatus == TaskSyncUiStatus.SYNCING) return
        syncStatus = TaskSyncUiStatus.SYNCING
        val previousSave = persistenceJob
        scope.launch {
            previousSave?.join()
            when (coordinator.sync()) {
                is TaskSyncResult.Success -> {
                    val syncRepository = repository as? SyncAwareTaskRepository
                    if (syncRepository != null) {
                        val snapshot = syncRepository.loadSnapshot()
                        tasks = snapshot.tasks
                        linkedAccountId = snapshot.linkedAccountId
                        pendingDeleteRemoteIds.clear()
                        pendingDeleteRemoteIds.addAll(snapshot.pendingDeleteRemoteIds)
                    }
                    syncStatus = TaskSyncUiStatus.SUCCESS
                }
                TaskSyncResult.SignedOut -> syncStatus = TaskSyncUiStatus.SIGNED_OUT
                TaskSyncResult.Offline -> syncStatus = TaskSyncUiStatus.OFFLINE
                is TaskSyncResult.AccountMismatch -> syncStatus = TaskSyncUiStatus.ACCOUNT_MISMATCH
                TaskSyncResult.Failed -> syncStatus = TaskSyncUiStatus.FAILED
            }
        }
    }

    /** Shows the change immediately and saves it in the background. */
    private fun update(newTasks: List<Task>) {
        if (!isLoaded || syncStatus == TaskSyncUiStatus.SYNCING) return
        val remainingIds = newTasks.mapTo(mutableSetOf()) { it.id }
        tasks.asSequence()
            .filter { it.id !in remainingIds }
            .mapNotNull { it.remoteId }
            .forEach(pendingDeleteRemoteIds::add)

        tasks = newTasks
        val deletionSnapshot = pendingDeleteRemoteIds.toSet()
        val previousSave = persistenceJob
        persistenceJob = scope.launch {
            previousSave?.join()
            val syncRepository = repository as? SyncAwareTaskRepository
            if (syncRepository != null) {
                syncRepository.saveSnapshot(
                    TaskStoreSnapshot(
                        tasks = newTasks,
                        linkedAccountId = linkedAccountId,
                        pendingDeleteRemoteIds = deletionSnapshot
                    )
                )
            } else {
                repository.save(newTasks)
            }
        }
    }
}
