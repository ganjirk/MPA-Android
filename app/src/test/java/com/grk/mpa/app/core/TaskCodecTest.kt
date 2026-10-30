package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCodecTest {
    @Test
    fun emptyListRoundTrips() {
        assertEquals("", TaskCodec.encode(emptyList()))
        assertTrue(TaskCodec.decode("").isEmpty())
    }

    @Test
    fun tasksRoundTrip() {
        val tasks = listOf(
            Task(1, "Buy milk", false, 1_700_000_000_000),
            Task(2, "Done thing", true, 5)
        )
        assertEquals(tasks, TaskCodec.decode(TaskCodec.encode(tasks)))
    }

    @Test
    fun specialCharactersSurviveTheRoundTrip() {
        val title = "Tab\there, new\nline, 100% sure & +plus, ünïcödé, 日本語, 😀"
        val tasks = listOf(Task(7, title, true, 1))
        val encoded = TaskCodec.encode(tasks)
        assertEquals("one line per task", 1, encoded.lines().size)
        assertEquals(tasks, TaskCodec.decode(encoded))
    }

    @Test
    fun legacyFourColumnRecordsRemainLocalOnly() {
        val legacy = "42\ttrue\t1234\tOld+task"
        assertEquals(
            listOf(Task(42, "Old task", true, 1234)),
            TaskCodec.decode(legacy)
        )
    }

    @Test
    fun remoteIdAndSyncStateRoundTrip() {
        val tasks = listOf(
            Task(8, "Server task", false, 77, remoteId = 901, syncState = TaskSyncState.SYNCED),
            Task(9, "Edited task", true, 88, remoteId = 902, syncState = TaskSyncState.PENDING_UPDATE),
            Task(10, "Device-only task", false, 99)
        )
        assertEquals(tasks, TaskCodec.decode(TaskCodec.encode(tasks)))
    }

    @Test
    fun invalidRemoteSyncMetadataIsSkipped() {
        val valid = TaskCodec.encode(listOf(Task(1, "Keep", false, 1)))
        val invalidWithoutRemoteId = "2\tfalse\t1\tNo+remote\t\tSYNCED"
        val invalidRemoteId = "3\tfalse\t1\tBad+remote\t0\tSYNCED"
        assertEquals(
            listOf(Task(1, "Keep", false, 1)),
            TaskCodec.decode(listOf(invalidWithoutRemoteId, invalidRemoteId, valid).joinToString("\n"))
        )
    }

    @Test
    fun malformedLinesAreSkipped() {
        val good = TaskCodec.encode(listOf(Task(1, "Keep me", false, 1)))
        val text = listOf(
            "garbage",
            "x\tfalse\t1\tTitle",
            "2\tmaybe\t1\tTitle",
            "3\tfalse\tnope\tTitle",
            "4\tfalse\t1\t",
            "5\tfalse\t1\t%ZZ",
            good
        ).joinToString("\n")
        assertEquals(listOf(Task(1, "Keep me", false, 1)), TaskCodec.decode(text))
    }
}
