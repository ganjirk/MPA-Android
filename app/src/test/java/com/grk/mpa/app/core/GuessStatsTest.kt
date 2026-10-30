package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuessStatsTest {
    private fun record(level: GuessLevel, attempts: Int, won: Boolean, at: Long = 0) =
        GuessRecord(level, attempts, won, at)

    @Test
    fun emptyHistoryHasZeroEverything() {
        val summary = GuessStats.summarize(emptyList())
        assertEquals(GuessSummary.EMPTY, summary)
        assertEquals(0, summary.winRatePercent)
    }

    @Test
    fun summaryCountsWinsLossesAndRate() {
        val summary = GuessStats.summarize(
            listOf(
                record(GuessLevel.NORMAL, 3, true),
                record(GuessLevel.NORMAL, 5, false),
                record(GuessLevel.HARD, 4, true)
            )
        )
        assertEquals(3, summary.total)
        assertEquals(2, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(67, summary.winRatePercent)
    }

    @Test
    fun winRateIsRoundedToAWholePercent() {
        val oneInThree = GuessStats.summarize(
            listOf(record(GuessLevel.NORMAL, 1, true), record(GuessLevel.NORMAL, 5, false), record(GuessLevel.NORMAL, 5, false))
        )
        assertEquals(33, oneInThree.winRatePercent)
    }

    @Test
    fun bestAttemptsAreKeptPerLevelAndOnlyForWins() {
        val summary = GuessStats.summarize(
            listOf(
                record(GuessLevel.NORMAL, 4, true),
                record(GuessLevel.NORMAL, 2, true),
                record(GuessLevel.NORMAL, 1, false),
                record(GuessLevel.MEDIUM, 5, false)
            )
        )
        assertEquals(mapOf(GuessLevel.NORMAL to 2), summary.bestAttempts)
    }

    @Test
    fun appendKeepsOnlyTheNewestRecords() {
        val many = (1..GuessStats.MAX_RECORDS).map { record(GuessLevel.NORMAL, 1, true, it.toLong()) }
        val updated = GuessStats.append(many, record(GuessLevel.NORMAL, 2, false, 9999))
        assertEquals(GuessStats.MAX_RECORDS, updated.size)
        assertEquals(9999L, updated.last().playedAtMillis)
        assertEquals(2L, updated.first().playedAtMillis)
    }

    @Test
    fun codecRoundTrips() {
        val records = listOf(
            record(GuessLevel.NORMAL, 3, true, 1_700_000_000_000),
            record(GuessLevel.HARD, 5, false, 5)
        )
        assertEquals(records, GuessRecordCodec.decode(GuessRecordCodec.encode(records)))
        assertTrue(GuessRecordCodec.decode("").isEmpty())
    }

    @Test
    fun codecSkipsMalformedLines() {
        val good = GuessRecordCodec.encode(listOf(record(GuessLevel.MEDIUM, 2, true, 7)))
        val text = listOf(
            "junk",
            "EXTREME\t3\ttrue\t1",
            "NORMAL\tx\ttrue\t1",
            "NORMAL\t0\ttrue\t1",
            "NORMAL\t3\tmaybe\t1",
            "NORMAL\t3\ttrue\tnope",
            good
        ).joinToString("\n")
        assertEquals(listOf(record(GuessLevel.MEDIUM, 2, true, 7)), GuessRecordCodec.decode(text))
    }
}
