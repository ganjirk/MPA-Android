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
