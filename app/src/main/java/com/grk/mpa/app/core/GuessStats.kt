package com.grk.mpa.app.core

import kotlin.math.roundToInt

/** One finished game of Number Guess. */
data class GuessRecord(
    val level: GuessLevel,
    val attempts: Int,
    val won: Boolean,
    val playedAtMillis: Long
)

/** The numbers shown in "Your stats", like the website's My stats page. */
data class GuessSummary(
    val total: Int,
    val wins: Int,
    /** The fewest attempts of any win, per level. Levels without a win are missing. */
    val bestAttempts: Map<GuessLevel, Int>
) {
    val losses: Int get() = total - wins
    val winRatePercent: Int get() = if (total == 0) 0 else (100.0 * wins / total).roundToInt()

    companion object {
        val EMPTY = GuessSummary(0, 0, emptyMap())
    }
}

object GuessStats {
    /** Only this many of the newest records are kept. */
    const val MAX_RECORDS = 500

    fun summarize(records: List<GuessRecord>): GuessSummary = GuessSummary(
        total = records.size,
        wins = records.count { it.won },
        bestAttempts = records.filter { it.won }
            .groupBy { it.level }
            .mapValues { (_, wins) -> wins.minOf { it.attempts } }
    )

    fun append(records: List<GuessRecord>, record: GuessRecord): List<GuessRecord> =
        (records + record).takeLast(MAX_RECORDS)
}

/** Stores records as plain text, one per line: `LEVEL <TAB> attempts <TAB> won <TAB> millis`. Bad lines are skipped. */
object GuessRecordCodec {
    private const val SEPARATOR = '\t'

    fun encode(records: List<GuessRecord>): String = records.joinToString("\n") {
        listOf(it.level.name, it.attempts, it.won, it.playedAtMillis).joinToString(SEPARATOR.toString())
    }

    fun decode(text: String): List<GuessRecord> = text.lineSequence().mapNotNull { line ->
        val parts = line.split(SEPARATOR)
        if (parts.size != 4) return@mapNotNull null
        val level = GuessLevel.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
        val attempts = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val won = when (parts[2]) {
            "true" -> true
            "false" -> false
            else -> return@mapNotNull null
        }
        val playedAt = parts[3].toLongOrNull() ?: return@mapNotNull null
        GuessRecord(level, attempts, won, playedAt)
    }.toList()
}
