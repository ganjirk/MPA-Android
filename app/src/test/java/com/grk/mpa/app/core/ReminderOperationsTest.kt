package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderOperationsTest {
    private val sample = listOf(Reminder(1, "Pay bill", 200L), Reminder(2, "Call home", 100L, true))

    @Test fun normalizesAndRejectsBlankTitles() {
        assertEquals("Pay the bill", ReminderOperations.normalizeTitle(" Pay   the\nbill "))
        assertNull(ReminderOperations.normalizeTitle("  "))
    }

    @Test fun addsWithNextIdAndChronologicalOrder() {
        val result = ReminderOperations.add(sample, "Meeting", 50L)
        assertEquals(listOf(50L, 100L, 200L), result.map { it.triggerAtMillis })
        assertEquals(3L, result.first().id)
    }

    @Test fun editsOnlyMatchingReminder() {
        val result = ReminderOperations.update(sample, 1, "Updated", 300L)
        assertEquals("Updated", result.first { it.id == 1L }.title)
        assertEquals(300L, result.first { it.id == 1L }.triggerAtMillis)
        assertEquals("Call home", result.first { it.id == 2L }.title)
    }

    @Test fun completesAndDeletes() {
        assertEquals(true, ReminderOperations.complete(sample, 1).first { it.id == 1L }.isCompleted)
        assertEquals(listOf(2L), ReminderOperations.delete(sample, 1).map { it.id })
    }

    @Test fun codecRoundTripsTitlesWithSeparators() {
        val original = listOf(Reminder(9, "Pay\tbill\nnow", 1234L))
        assertEquals(original, ReminderCodec.decode(ReminderCodec.encode(original)))
    }

    @Test fun separatesUpcomingAndCompleted() {
        assertEquals(listOf(1L), ReminderOperations.upcoming(sample).map { it.id })
        assertEquals(listOf(2L), ReminderOperations.completed(sample).map { it.id })
    }
}
