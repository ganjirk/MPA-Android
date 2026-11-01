package com.grk.mpa.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

/** Everything "Your week" is built from. All of it already lives on the device. */
data class RecapInput(
    val tasks: List<Task> = emptyList(),
    val expenses: ExpenseData = ExpenseData(),
    val guessRecords: List<GuessRecord> = emptyList(),
    val matchResults: List<MatchResult> = emptyList(),
    val ticTacToe: TicTacToeStats = TicTacToeStats(),
    /** How each day felt, 1 (rough) to 5 (great). */
    val moods: Map<LocalDate, Int> = emptyMap()
)

data class DayRecap(
    val date: LocalDate,
    val spentPaise: Long,
    /** Height of the spending bar, 0 to 100, relative to the week's biggest day. */
    val barPercent: Int,
    val mood: Int?,
    val active: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean
)

data class CategoryShare(val category: ExpenseCategory, val totalPaise: Long, val percent: Int)

data class BudgetMeter(val spentPaise: Long, val amountPaise: Long, val percent: Int, val over: Boolean)

/** A sentence of the summary, kept as data so the screen can translate it. */
sealed interface RecapHighlight {
    data class TasksFinished(val count: Int, val previous: Int) : RecapHighlight
    data class TasksAdded(val count: Int) : RecapHighlight
    data class Spent(
        val totalPaise: Long,
        /** Change against the earlier period in percent, or null when there was nothing to compare with. */
        val changePercent: Int?,
        val topCategory: ExpenseCategory?,
        val topPaise: Long
    ) : RecapHighlight

    data class Games(val count: Int, val wins: Int) : RecapHighlight
    data class Active(val days: Int, val of: Int) : RecapHighlight
    data class AverageMood(val mood: Int) : RecapHighlight
}

data class WeekRecap(
    val start: LocalDate,
    val end: LocalDate,
    val isCurrent: Boolean,
    val days: List<DayRecap>,
    val tasksDone: Int,
    val tasksAdded: Int,
    val spentPaise: Long,
    val spentPrevPaise: Long,
    val topCategories: List<CategoryShare>,
    val games: Int,
    val gamesPrev: Int,
    val wins: Int,
    val activeDays: Int,
    val daysElapsed: Int,
    val moodCount: Int,
    val averageMood: Int?,
    val highlights: List<RecapHighlight>,
    val budget: BudgetMeter?,
    val isEmpty: Boolean
)

object Moods {
    val EMOJI = mapOf(1 to "😞", 2 to "🙁", 3 to "😐", 4 to "🙂", 5 to "😄")
    val RANGE = 1..5
}

/**
 * Builds the "Your week" recap, ported from the website (`HOME/recap.py`). Weeks run Monday to Sunday.
 * A week still in progress is compared with the same days of the week before.
 */
object WeekRecaps {
    fun weekStart(day: LocalDate): LocalDate = day.minusDays((day.dayOfWeek.value - 1).toLong())

    private class Counts(val done: Int, val added: Int, val games: Int, val wins: Int, val activeDates: Set<LocalDate>)

    fun build(input: RecapInput, start: LocalDate, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): WeekRecap {
        fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

        val end = start.plusDays(6)
        val isCurrent = !today.isBefore(start) && !today.isAfter(end)
        val daysElapsed = when {
            isCurrent -> (today.toEpochDay() - start.toEpochDay()).toInt() + 1
            end.isBefore(today) -> 7
            else -> 0
        }
        val prevStart = start.minusDays(7)
        val compareDays = if (isCurrent) daysElapsed else 7

        fun counts(from: LocalDate, days: Int): Counts {
            val to = from.plusDays((days - 1).toLong())
            fun inRange(millis: Long) = millis > 0L && dateOf(millis) in from..to
            val doneDates = input.tasks.filter { it.isDone && inRange(it.completedAtMillis) }.map { dateOf(it.completedAtMillis) }
            val addedDates = input.tasks.filter { inRange(it.createdAtMillis) }.map { dateOf(it.createdAtMillis) }
            val guess = input.guessRecords.filter { inRange(it.playedAtMillis) }
            val match = input.matchResults.filter { inRange(it.completedAtMillis) }
            val localTtt = input.ticTacToe.recentMatches.filter { inRange(it.playedAtMillis) }
            val computerTtt = input.ticTacToe.recentComputerGames.filter { inRange(it.playedAtMillis) }
            val dates = buildSet {
                addAll(doneDates)
                addAll(addedDates)
                guess.forEach { add(dateOf(it.playedAtMillis)) }
                match.forEach { add(dateOf(it.completedAtMillis)) }
                localTtt.forEach { add(dateOf(it.playedAtMillis)) }
                computerTtt.forEach { add(dateOf(it.playedAtMillis)) }
            }
            return Counts(
                done = doneDates.size,
                added = addedDates.size,
                games = guess.size + match.size + localTtt.size + computerTtt.size,
                wins = guess.count { it.won } + computerTtt.count { it.result == GameResult.WIN },
                activeDates = dates
            )
        }

        val now = counts(start, 7)
        val before = counts(prevStart, compareDays)

        // Spending
        val weekExpenses = input.expenses.expenses.filter { it.spentOn in start..end }
        val perDay = weekExpenses.groupBy { it.spentOn }.mapValues { (_, list) -> list.sumOf { it.amountPaise } }
        val spent = perDay.values.sum()
        val prevEnd = prevStart.plusDays((compareDays - 1).toLong())
        val spentPrev = input.expenses.expenses.filter { it.spentOn in prevStart..prevEnd }.sumOf { it.amountPaise }
        val topCategories = weekExpenses.groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amountPaise } }
            .sortedByDescending { it.second }
            .take(4)
            .map { (category, total) -> CategoryShare(category, total, if (spent > 0) (total * 100.0 / spent).roundToInt() else 0) }

        val moods = input.moods.filterKeys { it in start..end }
        val activeDates = now.activeDates + perDay.keys + moods.keys

        val biggest = perDay.values.maxOrNull() ?: 0L
        val days = (0 until 7).map { i ->
            val day = start.plusDays(i.toLong())
            val amount = perDay[day] ?: 0L
            DayRecap(
                date = day,
                spentPaise = amount,
                barPercent = if (biggest > 0) (amount * 100.0 / biggest).roundToInt() else 0,
                mood = moods[day],
                active = day in activeDates,
                isToday = day == today,
                isFuture = day.isAfter(today)
            )
        }
        val activeCount = days.count { it.active }
        val averageMood = if (moods.isNotEmpty()) moods.values.average().roundToInt() else null

        val highlights = buildList {
            if (now.done > 0) add(RecapHighlight.TasksFinished(now.done, before.done))
            else if (now.added > 0) add(RecapHighlight.TasksAdded(now.added))
            if (spent > 0) {
                val change = if (spentPrev > 0) ((spent - spentPrev) * 100.0 / spentPrev).roundToInt() else null
                val top = topCategories.firstOrNull()
                add(RecapHighlight.Spent(spent, change, top?.category, top?.totalPaise ?: 0L))
            }
            if (now.games > 0) add(RecapHighlight.Games(now.games, now.wins))
            if (daysElapsed > 0) add(RecapHighlight.Active(activeCount, if (isCurrent) daysElapsed else 7))
            if (averageMood != null) add(RecapHighlight.AverageMood(averageMood))
        }

        val budget = if (isCurrent) {
            val month = YearMonth.from(today)
            val amount = input.expenses.budgets[month]
            if (amount != null && amount > 0) {
                val monthSpent = input.expenses.expenses
                    .filter { YearMonth.from(it.spentOn) == month && !it.spentOn.isAfter(today) }
                    .sumOf { it.amountPaise }
                BudgetMeter(monthSpent, amount, minOf((monthSpent * 100.0 / amount).roundToInt(), 100), monthSpent > amount)
            } else null
        } else null

        return WeekRecap(
            start = start, end = end, isCurrent = isCurrent, days = days,
            tasksDone = now.done, tasksAdded = now.added,
            spentPaise = spent, spentPrevPaise = spentPrev, topCategories = topCategories,
            games = now.games, gamesPrev = before.games, wins = now.wins,
            activeDays = activeCount, daysElapsed = daysElapsed,
            moodCount = moods.size, averageMood = averageMood,
            highlights = highlights, budget = budget,
            isEmpty = now.done == 0 && now.added == 0 && spent == 0L && now.games == 0 && moods.isEmpty()
        )
    }
}

/** Plain text storage for moods: one line per day, `yyyy-mm-dd <TAB> 1..5`. Bad lines are skipped. */
object MoodCodec {
    const val MAX_DAYS = 400

    fun encode(moods: Map<LocalDate, Int>): String =
        moods.entries.sortedByDescending { it.key }.take(MAX_DAYS).joinToString("\n") { "${it.key}\t${it.value}" }

    fun decode(text: String): Map<LocalDate, Int> = text.lineSequence().mapNotNull { line ->
        val p = line.split("\t")
        if (p.size != 2) return@mapNotNull null
        val date = runCatching { LocalDate.parse(p[0]) }.getOrNull() ?: return@mapNotNull null
        val mood = p[1].toIntOrNull()?.takeIf { it in Moods.RANGE } ?: return@mapNotNull null
        date to mood
    }.toMap()
}
