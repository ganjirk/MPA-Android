package com.grk.mpa.app.core

/** Pure functions on an immutable task list. */
object TaskOperations {
    const val MAX_TITLE_LENGTH = 100

    /** Trims, collapses whitespace and limits the length; null when nothing is left. */
    fun normalizeTitle(raw: String): String? =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE_LENGTH).trim().takeIf { it.isNotEmpty() }

    fun add(tasks: List<Task>, rawTitle: String, nowMillis: Long): List<Task> {
        val title = normalizeTitle(rawTitle) ?: return tasks
        val nextId = (tasks.maxOfOrNull { it.id } ?: 0L) + 1
        return tasks + Task(id = nextId, title = title, isDone = false, createdAtMillis = nowMillis)
    }

    fun toggle(tasks: List<Task>, id: Long): List<Task> =
        tasks.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }

    fun delete(tasks: List<Task>, id: Long): List<Task> = tasks.filterNot { it.id == id }

    fun clearCompleted(tasks: List<Task>): List<Task> = tasks.filterNot { it.isDone }

    fun filter(tasks: List<Task>, filter: TaskFilter): List<Task> = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.ACTIVE -> tasks.filterNot { it.isDone }
        TaskFilter.DONE -> tasks.filter { it.isDone }
    }

    fun activeCount(tasks: List<Task>): Int = tasks.count { !it.isDone }
}
