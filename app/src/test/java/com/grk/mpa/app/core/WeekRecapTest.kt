package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class WeekRecapTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 26)
    private val wednesday = LocalDate.of(2026, 10, 28)

    private fun millis(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000L

    private fun expense(id: Long, day: LocalDate, rupees: Long, category: ExpenseCategory = ExpenseCategory.FOOD) =
        Expense(id, rupees * 100, category, "x", day)

    private fun build(input: RecapInput, start: LocalDate = monday, today: LocalDate = wednesday) =
        WeekRecaps.build(input, start, today, zone)

    @Test
    fun weekStartIsTheMonday() {
        assertEquals(monday, WeekRecaps.weekStart(monday))
        assertEquals(monday, WeekRecaps.weekStart(LocalDate.of(2026, 11, 1)))
        assertEquals(monday, WeekRecaps.weekStart(wednesday))
    }

    @Test
    fun emptyWeekIsEmpty() {
        val recap = build(RecapInput())
        assertTrue(recap.isEmpty)
        assertEquals(0, recap.activeDays)
        assertNull(recap.budget)
        assertEquals(7, recap.days.size)
        assertTrue(recap.days[2].isToday)
        assertTrue(recap.days[3].isFuture)
    }

    @Test
    fun tasksAreComparedWithTheSameDaysOfLastWeek() {
        val tasks = listOf(
            Task(1, "a", true, 1, completedAtMillis = millis(monday)),
            Task(2, "b", true, 1, completedAtMillis = millis(monday.plusDays(1))),
            Task(3, "c", true, 1, completedAtMillis = millis(monday.minusDays(7))),
            Task(4, "d", true, 1, completedAtMillis = millis(monday.minusDays(5))),
            // Friday of last week is after the compared days (Mon to Wed).
            Task(5, "e", true, 1, completedAtMillis = millis(monday.minusDays(3))),
            Task(6, "not done", false, millis(monday))
        )
        val recap = build(RecapInput(tasks = tasks))
        assertEquals(2, recap.tasksDone)
        assertEquals(1, recap.tasksAdded)
        val highlight = recap.highlights.filterIsInstance<RecapHighlight.TasksFinished>().single()
        assertEquals(2, highlight.count)
        assertEquals(2, highlight.previous)
    }

    @Test
    fun tasksWithoutACompletionTimeAreNotCounted() {
        val recap = build(RecapInput(tasks = listOf(Task(1, "old", true, 0, completedAtMillis = 0))))
        assertEquals(0, recap.tasksDone)
        assertTrue(recap.isEmpty)
    }

    @Test
    fun onlyAddedTasksGiveTheAddedHighlight() {
        val recap = build(RecapInput(tasks = listOf(Task(1, "new", false, millis(monday)))))
        assertEquals(listOf(RecapHighlight.TasksAdded(1)), recap.highlights.filterIsInstance<RecapHighlight.TasksAdded>())
        assertTrue(recap.highlights.none { it is RecapHighlight.TasksFinished })
    }

    @Test
    fun spendingBarsCategoriesAndChange() {
        val expenses = ExpenseData(
            expenses = listOf(
                expense(1, monday, 100, ExpenseCategory.FOOD),
                expense(2, monday.plusDays(1), 50, ExpenseCategory.TRANSPORT),
                expense(3, monday.plusDays(1), 50, ExpenseCategory.FOOD),
                expense(4, monday.minusDays(7), 100, ExpenseCategory.FOOD),
                // Beyond the compared days of last week.
                expense(5, monday.minusDays(2), 900, ExpenseCategory.FOOD)
            )
        )
        val recap = build(RecapInput(expenses = expenses))
        assertEquals(20_000L, recap.spentPaise)
        assertEquals(10_000L, recap.spentPrevPaise)
        assertEquals(100, recap.days[0].barPercent)
        assertEquals(100, recap.days[1].barPercent)
        assertEquals(0, recap.days[2].barPercent)
        assertEquals(listOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT), recap.topCategories.map { it.category })
        assertEquals(75, recap.topCategories[0].percent)
        val spent = recap.highlights.filterIsInstance<RecapHighlight.Spent>().single()
        assertEquals(100, spent.changePercent)
        assertEquals(ExpenseCategory.FOOD, spent.topCategory)
        assertEquals(15_000L, spent.topPaise)
    }

    @Test
    fun noChangeFigureWhenLastWeekHadNoSpending() {
        val recap = build(RecapInput(expenses = ExpenseData(listOf(expense(1, monday, 10)))))
        assertNull(recap.highlights.filterIsInstance<RecapHighlight.Spent>().single().changePercent)
    }

    @Test
    fun onlyFourCategoriesAreKept() {
        val list = ExpenseCategory.entries.mapIndexed { i, c -> expense(i + 1L, monday, 10L + i, c) }
        assertEquals(4, build(RecapInput(expenses = ExpenseData(list))).topCategories.size)
    }

    @Test
    fun gamesFromAllSourcesAreCounted() {
        val input = RecapInput(
            guessRecords = listOf(
                GuessRecord(GuessLevel.NORMAL, 3, true, millis(monday)),
                GuessRecord(GuessLevel.NORMAL, 7, false, millis(monday)),
                GuessRecord(GuessLevel.NORMAL, 7, true, millis(monday.minusDays(30)))
            ),
            matchResults = listOf(MatchResult(millis(wednesday), MatchDifficulty.EASY, 12, 40)),
            ticTacToe = TicTacToeStats(
                recentMatches = listOf(LocalMatchEntry(millis(monday), "A", "B", MatchWinner.X)),
                recentComputerGames = listOf(
                    ComputerGameEntry(millis(monday), GameResult.WIN),
                    ComputerGameEntry(millis(monday), GameResult.LOSS)
                )
            )
        )
        val recap = build(input)
        assertEquals(5, recap.games)
        assertEquals(2, recap.wins)
        assertFalse(recap.isEmpty)
        assertEquals(RecapHighlight.Games(5, 2), recap.highlights.filterIsInstance<RecapHighlight.Games>().single())
    }

    @Test
    fun activeDaysAndMood() {
        val input = RecapInput(
            tasks = listOf(Task(1, "a", true, 1, completedAtMillis = millis(monday))),
            expenses = ExpenseData(listOf(expense(1, monday, 5), expense(2, monday.plusDays(1), 5))),
            moods = mapOf(wednesday to 5, monday to 4, monday.minusDays(7) to 1)
        )
        val recap = build(input)
        assertEquals(3, recap.activeDays)
        assertEquals(3, recap.daysElapsed)
        assertEquals(2, recap.moodCount)
        assertEquals(5, recap.averageMood) // 4.5 rounds up
        assertEquals(5, recap.days[2].mood)
        assertEquals(RecapHighlight.Active(3, 3), recap.highlights.filterIsInstance<RecapHighlight.Active>().single())
    }

    @Test
    fun budgetMeterOnlyForTheCurrentWeek() {
        val data = ExpenseData(
            expenses = listOf(expense(1, monday, 300), expense(2, LocalDate.of(2026, 10, 3), 400)),
            budgets = mapOf(YearMonth.of(2026, 10) to 50_000L)
        )
        val current = build(RecapInput(expenses = data)).budget
        assertNotNull(current)
        assertEquals(70_000L, current!!.spentPaise)
        assertTrue(current.over)
        assertEquals(100, current.percent)

        val past = build(RecapInput(expenses = data), start = monday.minusDays(7))
        assertNull(past.budget)
    }

    @Test
    fun pastWeekCompareDaysAreTheWholeWeek() {
        val start = monday.minusDays(7)
        val recap = build(RecapInput(), start = start)
        assertFalse(recap.isCurrent)
        assertEquals(7, recap.daysElapsed)
        assertTrue(recap.days.none { it.isFuture })
    }
}

class MoodCodecTest {
    @Test
    fun roundTrips() {
        val moods = mapOf(LocalDate.of(2026, 10, 28) to 5, LocalDate.of(2026, 10, 27) to 2)
        assertEquals(moods, MoodCodec.decode(MoodCodec.encode(moods)))
        assertEquals("", MoodCodec.encode(emptyMap()))
        assertTrue(MoodCodec.decode("").isEmpty())
    }

    @Test
    fun badLinesAreSkipped() {
        val text = "2026-10-28\t4\nnonsense\n2026-10-27\t9\n2026-13-01\t3\n2026-10-26\tx\n2026-10-25\t1"
        assertEquals(
            mapOf(LocalDate.of(2026, 10, 28) to 4, LocalDate.of(2026, 10, 25) to 1),
            MoodCodec.decode(text)
        )
    }

    @Test
    fun keepsOnlyTheNewestDays() {
        val start = LocalDate.of(2026, 1, 1)
        val moods = (0 until MoodCodec.MAX_DAYS + 10).associate { start.plusDays(it.toLong()) to 3 }
        val decoded = MoodCodec.decode(MoodCodec.encode(moods))
        assertEquals(MoodCodec.MAX_DAYS, decoded.size)
        assertFalse(decoded.containsKey(start))
    }
}
