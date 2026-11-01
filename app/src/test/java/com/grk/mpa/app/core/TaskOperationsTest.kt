package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TaskOperationsTest {
    private val sample = listOf(
        Task(1, "Buy milk", isDone = false, createdAtMillis = 10),
        Task(2, "Call mum", isDone = true, createdAtMillis = 20),
        Task(5, "Write report", isDone = false, createdAtMillis = 30)
    )


    @Test
    fun togglingSyncedTaskMarksItPendingUpdate() {
        val original = Task(
            id = 3,
            title = "Remote",
            remoteId = 71,
            syncState = TaskSyncState.SYNCED
        )
        val toggled = TaskOperations.toggle(listOf(original), original.id).single()
        assertEquals(true, toggled.isDone)
        assertEquals(71L, toggled.remoteId)
        assertEquals(TaskSyncState.PENDING_UPDATE, toggled.syncState)
    }

    @Test
    fun togglingLocalTaskKeepsItLocalOnly() {
        val original = Task(id = 4, title = "Local")
        val toggled = TaskOperations.toggle(listOf(original), original.id).single()
        assertNull(toggled.remoteId)
        assertEquals(TaskSyncState.LOCAL_ONLY, toggled.syncState)
    }

    @Test
    fun togglingAlreadyPendingTaskKeepsPendingState() {
        val original = Task(
            id = 5,
            title = "Pending",
            remoteId = 72,
            syncState = TaskSyncState.PENDING_UPDATE
        )
        val toggled = TaskOperations.toggle(listOf(original), original.id).single()
        assertEquals(TaskSyncState.PENDING_UPDATE, toggled.syncState)
    }

    @Test
    fun normalizeTrimsAndCollapsesWhitespace() {
        assertEquals("Buy fresh milk", TaskOperations.normalizeTitle("  Buy   fresh\n milk  "))
    }

    @Test
    fun normalizeRejectsBlankAndLimitsLength() {
        assertNull(TaskOperations.normalizeTitle("   \n\t"))
        assertEquals(TaskOperations.MAX_TITLE_LENGTH, TaskOperations.normalizeTitle("x".repeat(500))!!.length)
    }

    @Test
    fun addAppendsWithNextIdAndTimestamp() {
        val result = TaskOperations.add(sample, "  New  ", nowMillis = 99)
        assertEquals(4, result.size)
        assertEquals(Task(6, "New", false, 99), result.last())
    }

    @Test
    fun addToEmptyListStartsAtOne() {
        assertEquals(1L, TaskOperations.add(emptyList(), "First", 0).single().id)
    }

    @Test
    fun addIgnoresBlankTitles() {
        assertSame(sample, TaskOperations.add(sample, "   ", 99))
    }

    @Test
    fun toggleFlipsOnlyTheMatchingTask() {
        val result = TaskOperations.toggle(sample, 1)
        assertEquals(listOf(true, true, false), result.map { it.isDone })
        assertEquals(listOf(false, false, false), TaskOperations.toggle(result, 1).let { TaskOperations.toggle(it, 2) }.map { it.isDone })
    }

    @Test
    fun toggleUnknownIdChangesNothing() {
        assertEquals(sample, TaskOperations.toggle(sample, 42))
    }

    @Test
    fun deleteRemovesOnlyTheMatchingTask() {
        assertEquals(listOf(1L, 5L), TaskOperations.delete(sample, 2).map { it.id })
    }

    @Test
    fun clearCompletedKeepsActiveTasks() {
        assertEquals(listOf(1L, 5L), TaskOperations.clearCompleted(sample).map { it.id })
    }

    @Test
    fun filtersSelectTheRightTasks() {
        assertEquals(3, TaskOperations.filter(sample, TaskFilter.ALL).size)
        assertEquals(listOf(1L, 5L), TaskOperations.filter(sample, TaskFilter.ACTIVE).map { it.id })
        assertEquals(listOf(2L), TaskOperations.filter(sample, TaskFilter.DONE).map { it.id })
        assertEquals(2, TaskOperations.activeCount(sample))
    }

    @Test
    fun togglingRecordsAndClearsTheCompletionTime() {
        val tasks = listOf(Task(1, "a", false, 5))
        val done = TaskOperations.toggle(tasks, 1, 777)
        assertEquals(777L, done.single().completedAtMillis)
        val undone = TaskOperations.toggle(done, 1, 999)
        assertEquals(0L, undone.single().completedAtMillis)
    }
}
