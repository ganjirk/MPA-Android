package com.grk.mpa.app.ui.productivity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.Task
import com.grk.mpa.app.core.TaskFilter
import com.grk.mpa.app.core.TaskOperations
import com.grk.mpa.app.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 */
class TasksViewModel(
    private val repository: TaskRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var newTitle by mutableStateOf("")
        private set
    var filter by mutableStateOf(TaskFilter.ALL)
        private set

    val visibleTasks: List<Task> get() = TaskOperations.filter(tasks, filter)
    val activeCount: Int get() = TaskOperations.activeCount(tasks)
    val hasCompleted: Boolean get() = tasks.any { it.isDone }
    val canAdd: Boolean get() = isLoaded && TaskOperations.normalizeTitle(newTitle) != null

    init {
        scope.launch {
            tasks = repository.load()
            isLoaded = true
        }
    }

    fun onNewTitleChange(text: String) {
        newTitle = text.take(TaskOperations.MAX_TITLE_LENGTH)
    }

    fun onFilterChange(newFilter: TaskFilter) {
        filter = newFilter
    }

    fun addTask() {
        if (!canAdd) return
        update(TaskOperations.add(tasks, newTitle, clock()))
        newTitle = ""
    }

    fun toggle(id: Long) = update(TaskOperations.toggle(tasks, id))

    fun delete(id: Long) = update(TaskOperations.delete(tasks, id))

    fun clearCompleted() = update(TaskOperations.clearCompleted(tasks))

    /** Shows the change immediately and saves it in the background. */
    private fun update(newTasks: List<Task>) {
        if (!isLoaded) return
        tasks = newTasks
        scope.launch { repository.save(newTasks) }
    }
}
